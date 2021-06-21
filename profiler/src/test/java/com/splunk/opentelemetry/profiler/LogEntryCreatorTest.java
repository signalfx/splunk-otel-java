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

import static com.splunk.opentelemetry.profiler.LogEntryCreator.PROFILING_SOURCE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.LINKED_SPAN_ID;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.LINKED_TRACE_ID;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_PERIOD;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.logs.LogEntry;
import com.splunk.opentelemetry.profiler.context.SpanLinkage;
import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import io.opentelemetry.api.common.Attributes;
import java.time.Instant;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.Test;

class LogEntryCreatorTest {

  @Test
  void testCreate() {
    Instant time = Instant.now();
    String stack = "the.stack";

    String spanId = "zzzyyyzzz";
    String traceId = "abc123";

    long threadId = 987L;
    EventType eventType = mock(EventType.class);
    RecordedEvent sourceEvent = mock(RecordedEvent.class);

    when(sourceEvent.getEventType()).thenReturn(eventType);
    when(eventType.getName()).thenReturn("GoodEventHere");

    SpanLinkage linkage = new SpanLinkage(traceId, spanId, threadId);
    Attributes attributes =
        Attributes.of(
            LINKED_TRACE_ID,
            traceId,
            LINKED_SPAN_ID,
            spanId,
            SOURCE_EVENT_NAME,
            "GoodEventHere",
            SOURCE_EVENT_PERIOD,
            -999L,
            SOURCE_TYPE,
            "otel.profiling");
    LogEntry expected =
        LogEntry.builder()
            .traceId(traceId)
            .spanId(spanId)
            .name(PROFILING_SOURCE)
            .body(stack)
            .time(time)
            .attributes(attributes)
            .build();

    StackToSpanLinkage linkedSpan = new StackToSpanLinkage(time, "the.stack", sourceEvent, linkage);

    LogEntryCreator creator = new LogEntryCreator();
    LogEntry result = creator.apply(linkedSpan);
    assertEquals(expected, result);
  }
}
