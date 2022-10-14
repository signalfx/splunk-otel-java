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

import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_MEMORY_SAMPLER_INTERVAL;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_TLAB_ENABLED;
import static com.splunk.opentelemetry.profiler.Configuration.DEFAULT_MEMORY_ENABLED;
import static com.splunk.opentelemetry.profiler.Configuration.DEFAULT_MEMORY_SAMPLING_INTERVAL;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.DATA_FORMAT;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.DATA_TYPE;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_TYPE;
import static com.splunk.opentelemetry.profiler.TLABProcessor.ALLOCATION_SIZE_KEY;
import static io.opentelemetry.sdk.testing.assertj.LogAssertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.allocation.exporter.AllocationEventExporter;
import com.splunk.opentelemetry.profiler.allocation.exporter.PlainTextAllocationEventExporter;
import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.context.SpanLinkage;
import com.splunk.opentelemetry.profiler.events.EventPeriods;
import com.splunk.opentelemetry.profiler.util.StackSerializer;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.InMemoryLogRecordExporter;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

class TLABProcessorTest {

  public static final long ONE_MB = 1024 * 1024L;
  public static final long FIVE_MB = 5 * 1024 * 1024L;
  public static final long THREAD_ID = 606L;
  public static final long OS_THREAD_ID = 0x707L;

  static final InMemoryLogRecordExporter logExporter = InMemoryLogRecordExporter.create();
  static final Logger otelLogger =
      SdkLoggerProvider.builder()
          .addLogRecordProcessor(SimpleLogRecordProcessor.create(logExporter))
          .build()
          .get("test");

  @BeforeEach
  void reset() {
    logExporter.reset();
  }

  @Test
  void testProcess() {
    Long tlabSize = FIVE_MB;
    tlabProcessTest(tlabSize);
  }

  @Test
  void testNullStack() {
    RecordedEvent event = mock(RecordedEvent.class);
    when(event.getStackTrace()).thenReturn(null); // just to be explicit

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getBoolean(CONFIG_KEY_TLAB_ENABLED, DEFAULT_MEMORY_ENABLED)).thenReturn(true);

    TLABProcessor processor = TLABProcessor.builder(config).build();
    processor.accept(event);
    // success, no NPEs
  }

  @Test
  void testProfilingDisabled() {
    RecordedEvent event =
        mock(
            RecordedEvent.class,
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                throw new IllegalStateException(
                    "RecordedEvent methods should not be called when TLAB profiling is not enabled");
              }
            });

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getBoolean(CONFIG_KEY_TLAB_ENABLED, DEFAULT_MEMORY_ENABLED)).thenReturn(false);

    TLABProcessor processor = TLABProcessor.builder(config).build();
    processor.accept(event);
  }

  @Test
  void testAllocationOutsideTLAB() {
    tlabProcessTest(null);
  }

  private void tlabProcessTest(Long tlabSize) {
    Instant now = Instant.now();
    String stackAsString =
        "\"mockingbird\" #606 nid=0x707\n"
            + "   java.lang.Thread.State: UNKNOWN\n"
            + "i am a serialized stack believe me";

    StackSerializer serializer = mock(StackSerializer.class);
    LogDataCommonAttributes commonAttrs = new LogDataCommonAttributes(new EventPeriods(x -> null));
    Clock clock = new MockClock(now);

    RecordedEvent event = createMockEvent(serializer, now, tlabSize);

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getBoolean(CONFIG_KEY_TLAB_ENABLED, DEFAULT_MEMORY_ENABLED)).thenReturn(true);

    SpanContext spanContext =
        SpanContext.create(
            TraceId.fromLongs(123, 456),
            SpanId.fromLong(123),
            TraceFlags.getSampled(),
            TraceState.getDefault());
    SpanContextualizer spanContextualizer = mock(SpanContextualizer.class);
    when(spanContextualizer.link(THREAD_ID)).thenReturn(new SpanLinkage(spanContext, THREAD_ID));

    AllocationEventExporter allocationEventExporter =
        PlainTextAllocationEventExporter.builder()
            .stackSerializer(serializer)
            .logEmitter(otelLogger)
            .commonAttributes(commonAttrs)
            .stackDepth(128)
            .build();

    TLABProcessor processor =
        TLABProcessor.builder(config)
            .allocationEventExporter(allocationEventExporter)
            .spanContextualizer(spanContextualizer)
            .build();

    processor.accept(event);

    assertThat(logExporter.getFinishedLogItems())
        .satisfiesExactly(
            log ->
                assertThat(log)
                    .hasSpanContext(spanContext)
                    .hasBody(stackAsString)
                    .hasEpochNanos(
                        TimeUnit.SECONDS.toNanos(now.getEpochSecond()) + clock.nanoTime())
                    .hasAttributes(
                        entry(SOURCE_TYPE, "otel.profiling"),
                        entry(SOURCE_EVENT_NAME, "tee-lab"),
                        entry(ALLOCATION_SIZE_KEY, ONE_MB),
                        entry(DATA_TYPE, ProfilingDataType.ALLOCATION.value()),
                        entry(DATA_FORMAT, Configuration.DataFormat.TEXT.value())));
  }

  private RecordedEvent createMockEvent(StackSerializer serializer, Instant now, Long tlabSize) {
    String stackAsString = "i am a serialized stack believe me";

    RecordedEvent event = mock(RecordedEvent.class);
    RecordedStackTrace stack = mock(RecordedStackTrace.class);
    EventType eventType = mock(EventType.class);
    RecordedThread mockThread = mock(RecordedThread.class);

    when(event.getStartTime()).thenReturn(now);
    when(event.getStackTrace()).thenReturn(stack);
    when(event.getEventType()).thenReturn(eventType);
    when(event.getLong("allocationSize")).thenReturn(ONE_MB);
    when(event.getThread()).thenReturn(mockThread);
    when(mockThread.getJavaThreadId()).thenReturn(THREAD_ID);
    when(mockThread.getOSThreadId()).thenReturn(OS_THREAD_ID);
    when(mockThread.getJavaName()).thenReturn("mockingbird");

    when(event.hasField("tlabSize")).thenReturn(tlabSize != null);
    if (tlabSize == null) {
      when(event.getLong("tlabSize")).thenThrow(NullPointerException.class);
    } else {
      when(event.getLong("tlabSize")).thenReturn(tlabSize);
    }
    when(eventType.getName()).thenReturn("tee-lab");
    when(serializer.serialize(stack)).thenReturn(stackAsString);

    return event;
  }

  private static final AttributeKey<String> SAMPLER_NAME_KEY =
      AttributeKey.stringKey("sampler.name");
  private static final AttributeKey<Long> SAMPLER_INTERVAL_KEY =
      AttributeKey.longKey("sampler.interval");

  @Test
  void testSampling() {
    int samplerInterval = 5;

    StackSerializer serializer = mock(StackSerializer.class);
    LogDataCommonAttributes commonAttrs = new LogDataCommonAttributes(new EventPeriods(x -> null));
    SpanContextualizer spanContextualizer = mock(SpanContextualizer.class);
    when(spanContextualizer.link(anyLong())).thenReturn(SpanLinkage.NONE);

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getBoolean(CONFIG_KEY_TLAB_ENABLED, DEFAULT_MEMORY_ENABLED)).thenReturn(true);
    when(config.getInt(CONFIG_KEY_MEMORY_SAMPLER_INTERVAL, DEFAULT_MEMORY_SAMPLING_INTERVAL))
        .thenReturn(samplerInterval);

    AllocationEventExporter allocationEventExporter =
        PlainTextAllocationEventExporter.builder()
            .stackSerializer(serializer)
            .logEmitter(otelLogger)
            .commonAttributes(commonAttrs)
            .stackDepth(128)
            .build();

    TLABProcessor processor =
        TLABProcessor.builder(config)
            .allocationEventExporter(allocationEventExporter)
            .spanContextualizer(spanContextualizer)
            .build();

    RecordedEvent event = createMockEvent(serializer, Instant.now(), null);

    for (int i = 0; i < samplerInterval + 2; i++) {
      processor.accept(event);

      if (i % samplerInterval == 0) {
        assertThat(logExporter.getFinishedLogItems())
            .satisfiesExactly(
                log ->
                    assertThat(log)
                        .hasAttributes(
                            entry(SOURCE_TYPE, "otel.profiling"),
                            entry(SOURCE_EVENT_NAME, "tee-lab"),
                            entry(SAMPLER_NAME_KEY, "Systematic sampler"),
                            entry(SAMPLER_INTERVAL_KEY, (long) samplerInterval),
                            entry(ALLOCATION_SIZE_KEY, ONE_MB),
                            entry(DATA_TYPE, ProfilingDataType.ALLOCATION.value()),
                            entry(DATA_FORMAT, Configuration.DataFormat.TEXT.value())));
      } else {
        assertThat(logExporter.getFinishedLogItems()).isEmpty();
      }

      logExporter.reset();
    }
  }

  private static class MockClock implements Clock {
    private final Instant now;

    public MockClock(Instant now) {
      this.now = now;
    }

    @Override
    public long now() {
      return now.toEpochMilli();
    }

    @Override
    public long nanoTime() {
      return now.getNano();
    }
  }
}
