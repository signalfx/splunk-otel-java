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

package com.splunk.opentelemetry.profiler;

import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.context.SpanLinkage;
import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import com.splunk.opentelemetry.profiler.exporter.CpuEventExporter;
import jdk.jfr.consumer.RecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadDumpProcessor {
  public static final String EVENT_NAME = "jdk.ThreadDump";
  private static final Logger logger = LoggerFactory.getLogger(ThreadDumpProcessor.class);
  private final SpanContextualizer contextualizer;
  private final CpuEventExporter cpuEventExporter;
  private final StackTraceFilter stackTraceFilter;
  private final boolean onlyTracingSpans;

  private ThreadDumpProcessor(Builder builder) {
    this.contextualizer = builder.contextualizer;
    this.cpuEventExporter = builder.cpuEventExporter;
    this.stackTraceFilter = builder.stackTraceFilter;
    this.onlyTracingSpans = builder.onlyTracingSpans;
  }

  public void accept(RecordedEvent event) {
    String eventName = event.getEventType().getName();
    logger.debug("Processing JFR event {}", eventName);
    String wallOfStacks = event.getString("result");

    ThreadDumpRegion stack = new ThreadDumpRegion(wallOfStacks, 0, 0);

    while (stack.findNextStack()) {
      if (!stackTraceFilter.test(stack)) {
        continue;
      }
      SpanLinkage linkage = contextualizer.link(stack);
      if (onlyTracingSpans && !linkage.getSpanContext().isValid()) {
        continue;
      }
      StackToSpanLinkage spanWithLinkage =
          new StackToSpanLinkage(
              event.getStartTime(), stack.getCurrentRegion(), eventName, linkage);
      cpuEventExporter.export(spanWithLinkage);
    }
  }

  public void flush() {
    cpuEventExporter.flush();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private SpanContextualizer contextualizer;
    private CpuEventExporter cpuEventExporter;
    private StackTraceFilter stackTraceFilter;
    private boolean onlyTracingSpans;

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

    public ThreadDumpProcessor build() {
      return new ThreadDumpProcessor(this);
    }
  }
}
