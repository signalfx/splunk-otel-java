/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.opentelemetry.profiler.threaddump;

import static java.util.logging.Level.FINE;

import com.splunk.opentelemetry.profiler.EventReader;
import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.context.SpanLinkage;
import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import com.splunk.opentelemetry.profiler.exporter.CpuEventExporter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.openjdk.jmc.common.item.IItem;

public class ThreadDumpProcessor {
  public static final String EVENT_NAME = "jdk.ThreadDump";
  private static final String LOCKED_PREFIX = "- locked ";
  private static final Logger logger = Logger.getLogger(ThreadDumpProcessor.class.getName());
  private final EventReader eventReader;
  private final SpanContextualizer contextualizer;
  private final CpuEventExporter cpuEventExporter;
  private final StackTraceFilter stackTraceFilter;
  private final boolean onlyTracingSpans;
  private final boolean locksEnabled;

  private ThreadDumpProcessor(Builder builder) {
    this.eventReader = builder.eventReader;
    this.contextualizer = builder.contextualizer;
    this.cpuEventExporter = builder.cpuEventExporter;
    this.stackTraceFilter = builder.stackTraceFilter;
    this.onlyTracingSpans = builder.onlyTracingSpans;
    this.locksEnabled = builder.locksEnabled;
  }

  public void accept(IItem event) {
    String eventName = event.getType().getIdentifier();
    logger.log(FINE, "Processing JFR event {0}", eventName);
    String wallOfStacks = eventReader.getThreadDumpResult(event);

    Map<String, String> lockToOwnerNameMapping = locksEnabled ? new HashMap<>() : null;
    List<StackToSpanLinkage> waitingStacks = locksEnabled ? new ArrayList<>() : null;

    ThreadDumpRegion.Iterator iterator = new ThreadDumpRegion.Iterator(wallOfStacks);
    ThreadDumpRegion stackRegion;
    while ((stackRegion = iterator.findNextStack()) != null) {
      if (!stackTraceFilter.test(stackRegion)) {
        continue;
      }

      SpanLinkage linkage = contextualizer.link(stackRegion);
      if (onlyTracingSpans && !linkage.getSpanContext().isValid()) {
        continue;
      }

      StackTraceData stackTrace =
          StackTraceParser.parse(
              stackRegion.getCurrentRegion(),
              1500100900,
              locksEnabled); // TODO: Get rid of stack depth here, leave it in exporter
      if (stackTrace == null) {
        continue;
      }
      if (locksEnabled) {
        maybeAddToLockOwners(stackTrace, lockToOwnerNameMapping);
      }

      StackToSpanLinkage spanWithLinkage =
          new StackToSpanLinkage(
              eventReader.getStartInstant(event), stackTrace, eventName, linkage);
      if (locksEnabled && stackTrace.getThreadLockData().getWaitingOn() != null) {
        waitingStacks.add(spanWithLinkage);
      } else {
        cpuEventExporter.export(spanWithLinkage);
      }
    }

    if (locksEnabled) {
      waitingStacks.forEach(
          span -> assignLockOwnerThreadName(span.getStackTrace(), lockToOwnerNameMapping));
      waitingStacks.forEach(cpuEventExporter::export);
    }
  }

  private void assignLockOwnerThreadName(
      StackTraceData stackTrace, Map<String, String> lockToOwnerNameMapping) {
    String waitingOn = stackTrace.getThreadLockData().getWaitingOn();
    if (waitingOn == null) {
      return;
    }
    String owner = lockToOwnerNameMapping.get(waitingOn);
    if (owner != null) {
      stackTrace.getThreadLockData().setLockOwner(owner);
    }
  }

  private void maybeAddToLockOwners(
      StackTraceData stackTrace, Map<String, String> lockToOwnerNameMapping) {
    if (!locksEnabled) {
      return;
    }
    if (stackTrace.getThreadName() == null) {
      return;
    }
    stackTrace
        .getThreadLockData()
        .getLockedMonitors()
        .forEach(
            lockId -> {
              lockToOwnerNameMapping.put(lockId, stackTrace.getThreadName());
            });
  }

  Map<String, String> buildLockToOwningThreadMapping(List<ThreadDumpRegion> stackRegions) {
    Map<String, String> lockOwners = new HashMap<>();

    for (ThreadDumpRegion stackRegion : stackRegions) {
      String threadDump = stackRegion.threadDump;
      int startIndex = stackRegion.startIndex;
      int endIndex = stackRegion.endIndex;

      int headerEnd = threadDump.indexOf('\n', startIndex);
      if (headerEnd == -1 || headerEnd > endIndex) {
        headerEnd = endIndex;
      }
      int threadNameEnd = threadDump.lastIndexOf('"', headerEnd - 1);
      if (threadNameEnd <= startIndex) {
        continue;
      }
      String threadName = threadDump.substring(startIndex + 1, threadNameEnd);

      for (int lineStart = headerEnd + 1; lineStart < endIndex; ) {
        int lineEnd = threadDump.indexOf('\n', lineStart);
        if (lineEnd == -1 || lineEnd > endIndex) {
          lineEnd = endIndex;
        }

        int contentStart = lineStart;
        while (contentStart < lineEnd && Character.isWhitespace(threadDump.charAt(contentStart))) {
          contentStart++;
        }

        if (threadDump.regionMatches(contentStart, LOCKED_PREFIX, 0, LOCKED_PREFIX.length())) {
          int lockStart = threadDump.indexOf('<', contentStart + LOCKED_PREFIX.length());
          int lockEnd = lockStart == -1 ? -1 : threadDump.indexOf('>', lockStart + 1);
          if (lockStart != -1 && lockEnd != -1 && lockEnd < lineEnd) {
            lockOwners.put(threadDump.substring(lockStart + 1, lockEnd), threadName);
          }
        }

        lineStart = lineEnd + 1;
      }
    }

    return lockOwners;
  }

  public void flush() {
    cpuEventExporter.flush();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private EventReader eventReader;
    private SpanContextualizer contextualizer;
    private CpuEventExporter cpuEventExporter;
    private StackTraceFilter stackTraceFilter;
    private boolean onlyTracingSpans;
    private boolean locksEnabled;

    public Builder eventReader(EventReader eventReader) {
      this.eventReader = eventReader;
      return this;
    }

    public Builder spanContextualizer(SpanContextualizer contextualizer) {
      this.contextualizer = contextualizer;
      return this;
    }

    public Builder cpuEventExporter(CpuEventExporter cpuEventExporter) {
      this.cpuEventExporter = cpuEventExporter;
      return this;
    }

    public Builder stackTraceFilter(StackTraceFilter stackTraceFilter) {
      this.stackTraceFilter = stackTraceFilter;
      return this;
    }

    public Builder onlyTracingSpans(boolean onlyTracingSpans) {
      this.onlyTracingSpans = onlyTracingSpans;
      return this;
    }

    public Builder locksEnabled(boolean locksEnabled) {
      this.locksEnabled = locksEnabled;
      return this;
    }

    public ThreadDumpProcessor build() {
      return new ThreadDumpProcessor(this);
    }
  }
}
