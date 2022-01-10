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

import static com.splunk.opentelemetry.profiler.LogDataCreator.PROFILING_SOURCE;
import static com.splunk.opentelemetry.profiler.LogExporterBuilder.INSTRUMENTATION_LIBRARY_INFO;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_PERIOD;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.context.SpanLinkage;
import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import com.splunk.opentelemetry.profiler.events.EventPeriods;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.data.LogDataBuilder;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LogDataCreatorTest {

  @Test
  void testCreate() {
    Instant time = Instant.now();
    String stack = "the.stack";

    SpanContext spanContext =
        SpanContext.create(
            "deadbeefdeadbeefdeadbeefdeadbeef",
            "0123012301230123",
            TraceFlags.getSampled(),
            TraceState.getDefault());
    Context context = Context.root().with(Span.wrap(spanContext));
    long threadId = 987L;
    String eventName = "GoodEventHere";

    EventPeriods periods = mock(EventPeriods.class);
    when(periods.getDuration(eventName)).thenReturn(Duration.ofMillis(606));

    SpanLinkage linkage = new SpanLinkage(spanContext, threadId);
    Resource resource = Resource.getDefault();
    Attributes attributes =
        Attributes.of(
            SOURCE_EVENT_NAME, eventName, SOURCE_EVENT_PERIOD, 606L, SOURCE_TYPE, "otel.profiling");
    LogData expected =
        LogDataBuilder.create(resource, INSTRUMENTATION_LIBRARY_INFO)
            .setContext(context)
            .setName(PROFILING_SOURCE)
            .setBody(stack)
            .setEpoch(time)
            .setAttributes(attributes)
            .build();

    StackToSpanLinkage linkedSpan = new StackToSpanLinkage(time, "the.stack", eventName, linkage);

    LogDataCreator creator =
        new LogDataCreator(new LogDataCommonAttributes(periods), Resource.getDefault());
    LogData result = creator.apply(linkedSpan);
    assertEquals(expected, result);
  }
}
