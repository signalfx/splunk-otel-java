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

package com.splunk.opentelemetry.profiler.exporter;

import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.DATA_FORMAT;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.DATA_TYPE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_PERIOD;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_TYPE;
import static io.opentelemetry.sdk.testing.assertj.LogAssertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.Configuration;
import com.splunk.opentelemetry.profiler.LogDataCommonAttributes;
import com.splunk.opentelemetry.profiler.ProfilingDataType;
import com.splunk.opentelemetry.profiler.context.SpanLinkage;
import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import com.splunk.opentelemetry.profiler.events.EventPeriods;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class PlainTextCpuEventExporterTest {

  static final InMemoryLogRecordExporter logExporter = InMemoryLogRecordExporter.create();
  static final Logger otelLogger =
      SdkLoggerProvider.builder()
          .addLogRecordProcessor(SimpleLogRecordProcessor.create(logExporter))
          .build()
          .get("test");

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
    long threadId = 987L;
    String eventName = "GoodEventHere";

    EventPeriods periods = mock(EventPeriods.class);
    when(periods.getDuration(eventName)).thenReturn(Duration.ofMillis(606));

    PlainTextCpuEventExporter cpuEventExporter =
        PlainTextCpuEventExporter.builder()
            .otelLogger(otelLogger)
            .commonAttributes(new LogDataCommonAttributes(periods))
            .build();

    SpanLinkage linkage = new SpanLinkage(spanContext, threadId);

    StackToSpanLinkage linkedSpan = new StackToSpanLinkage(time, "the.stack", eventName, linkage);

    cpuEventExporter.export(linkedSpan);

    assertThat(logExporter.getFinishedLogItems())
        .satisfiesExactly(
            log ->
                assertThat(log)
                    .hasSpanContext(spanContext)
                    .hasBody(stack)
                    .hasEpochNanos(toNanos(time))
                    .hasAttributes(
                        entry(SOURCE_TYPE, "otel.profiling"),
                        entry(SOURCE_EVENT_NAME, eventName),
                        entry(SOURCE_EVENT_PERIOD, 606L),
                        entry(DATA_TYPE, ProfilingDataType.CPU.value()),
                        entry(DATA_FORMAT, Configuration.DataFormat.TEXT.value())));
  }

  private static long toNanos(Instant instant) {
    return TimeUnit.SECONDS.toNanos(instant.getEpochSecond()) + instant.getNano();
  }
}
