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

import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.LINKED_SPAN_ID;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.LINKED_TRACE_ID;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_PERIOD;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_TYPE;

import com.splunk.opentelemetry.logs.LogEntry;
import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import com.splunk.opentelemetry.profiler.events.EventPeriods;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import java.time.Duration;
import java.util.function.Function;

/** Turns a linked stack into a new LogEntry instance */
public class LogEntryCreator implements Function<StackToSpanLinkage, LogEntry> {

  static final String PROFILING_SOURCE = "otel.profiling";

  private final EventPeriods periods;

  public LogEntryCreator(EventPeriods periods) {
    this.periods = periods;
  }

  @Override
  public LogEntry apply(StackToSpanLinkage linkedStack) {
    Attributes attributes = buildAttributes(linkedStack);
    LogEntry.Builder builder =
        LogEntry.builder()
            .name(PROFILING_SOURCE)
            .time(linkedStack.getTime())
            .body(linkedStack.getRawStack())
            .attributes(attributes);
    if (linkedStack.hasSpanInfo()) {
      builder.traceId(linkedStack.getTraceId()).spanId(linkedStack.getSpanId());
    }
    return builder.build();
  }

  private Attributes buildAttributes(StackToSpanLinkage linkedStack) {
    String sourceEvent = linkedStack.getSourceEventName();
    Duration eventPeriod = periods.getDuration(sourceEvent);

    // Note: It is currently believed that the span id and trace id on the LogRecord itself
    // do not get ingested correctly. Placing them here as attributes is a temporary workaround
    // until the collector/ingest can be remedied.

    AttributesBuilder builder =
        Attributes.builder()
            .put(LINKED_TRACE_ID, linkedStack.getTraceId())
            .put(LINKED_SPAN_ID, linkedStack.getSpanId())
            .put(SOURCE_TYPE, PROFILING_SOURCE)
            .put(SOURCE_EVENT_NAME, sourceEvent);

    if (!EventPeriods.UNKNOWN.equals(eventPeriod)) {
      builder.put(SOURCE_EVENT_PERIOD, eventPeriod.toMillis());
    }
    return builder.build();
  }
}
