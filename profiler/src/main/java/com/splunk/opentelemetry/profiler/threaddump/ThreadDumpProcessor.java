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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.openjdk.jmc.common.item.IItem;

public class ThreadDumpProcessor {
  public static final String EVENT_NAME = "jdk.ThreadDump";

  private static final Logger logger = Logger.getLogger(ThreadDumpProcessor.class.getName());

  private final EventReader eventReader;
  private final SpanContextualizer contextualizer;
  private final CpuEventExporter cpuEventExporter;
  private final StackTraceFilter stackTraceFilter;
  private final boolean onlyTracingSpans;
  private final int stackDepth;
  private final boolean locksEnabled;

  private ThreadDumpProcessor(Builder builder) {
    this.eventReader = builder.eventReader;
    this.contextualizer = builder.contextualizer;
    this.cpuEventExporter = builder.cpuEventExporter;
    this.stackTraceFilter = builder.stackTraceFilter;
    this.onlyTracingSpans = builder.onlyTracingSpans;
    this.stackDepth = builder.stackDepth;
    this.locksEnabled = builder.locksEnabled;
  }

  public void accept(IItem event) {
    String eventName = event.getType().getIdentifier();
    logger.log(FINE, "Processing JFR event {0}", eventName);
    String wallOfStacks = eventReader.getThreadDumpResult(event);

    Map<String, String> lockToOwnerNameMapping =
        locksEnabled
            ? DeadlockDataExtractor.extractLockOwners(wallOfStacks)
            : Collections.emptyMap();
    List<StackToSpanLinkage> waitingStacks =
        locksEnabled ? new ArrayList<>() : Collections.emptyList();

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
          StackTraceParser.parse(stackRegion.getCurrentRegion(), stackDepth, locksEnabled);
      if (stackTrace == null) {
        continue;
      }
      maybeAddToLockOwners(stackTrace, lockToOwnerNameMapping);

      StackToSpanLinkage spanWithLinkage =
          new StackToSpanLinkage(
              eventReader.getStartInstant(event), stackTrace, eventName, linkage);
      if (locksEnabled && stackTrace.getThreadLockData().getWaitingOn() != null) {
        waitingStacks.add(spanWithLinkage);
      } else {
        cpuEventExporter.export(spanWithLinkage);
      }
    }

    waitingStacks.forEach(
        spanLinkage -> {
          resolveLockOwnerThreadName(
              spanLinkage.getStackTrace().getThreadLockData(), lockToOwnerNameMapping);
          cpuEventExporter.export(spanLinkage);
        });
  }

  private void resolveLockOwnerThreadName(
      ThreadLockData threadLockData, Map<String, String> lockToOwnerNameMapping) {
    String waitingOn = threadLockData.getWaitingOn();
    if (waitingOn == null) {
      return;
    }
    String owner = lockToOwnerNameMapping.get(waitingOn);
    if (owner != null) {
      threadLockData.setLockOwner(owner);
    }
  }

  private void maybeAddToLockOwners(
      StackTraceData stackTrace, Map<String, String> lockToOwnerNameMapping) {
    if (!locksEnabled) {
      return;
    }

    String stackTraceThreadName = stackTrace.getThreadName();
    if (stackTraceThreadName == null) {
      return;
    }
    stackTrace
        .getThreadLockData()
        .getLockedMonitors()
        .forEach(
            lockId -> {
              lockToOwnerNameMapping.put(lockId, stackTraceThreadName);
            });
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
    private int stackDepth = 1024;
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

    public Builder stackDepth(int stackDepth) {
      this.stackDepth = stackDepth;
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
