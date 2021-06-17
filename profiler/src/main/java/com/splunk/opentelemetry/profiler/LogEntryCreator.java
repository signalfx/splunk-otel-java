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
import io.opentelemetry.api.common.Attributes;
import java.util.function.Function;

/** Turns a linked stack into a new LogEntry instance */
public class LogEntryCreator implements Function<StackToSpanLinkage, LogEntry> {

  static final String PROFILING_SOURCE = "otel.profiling";

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
    long eventPeriodMs = -999; // tbd

    // Note: It is currently believed that he span id and trace id on the LogRecord itself
    // do not get ingested correctly. Placing them here as attributes is a temporary workaround
    // until the collector/ingest can be remedied.

    return Attributes.of(
        LINKED_TRACE_ID,
        linkedStack.getTraceId(),
        LINKED_SPAN_ID,
        linkedStack.getSpanId(),
        SOURCE_TYPE,
        PROFILING_SOURCE,
        SOURCE_EVENT_NAME,
        sourceEvent,
        SOURCE_EVENT_PERIOD,
        eventPeriodMs);
  }
}
