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

import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_TLAB_ENABLED;
import static com.splunk.opentelemetry.profiler.Configuration.DEFAULT_MEMORY_ENABLED;
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
import com.splunk.opentelemetry.profiler.allocation.sampler.RateLimitingAllocationEventSampler;
import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.context.SpanLinkage;
import com.splunk.opentelemetry.profiler.events.EventPeriods;
import com.splunk.opentelemetry.profiler.util.StackSerializer;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TLABProcessorTest {

  public static final long ONE_MB = 1024 * 1024L;
  public static final long THREAD_ID = 606L;

  static final InMemoryLogRecordExporter logExporter = InMemoryLogRecordExporter.create();
  static final Logger otelLogger =
      SdkLoggerProvider.builder()
          .addLogRecordProcessor(SimpleLogRecordProcessor.create(logExporter))
          .build()
          .get("test");

  @Mock EventReader eventReader;

  @BeforeEach
  void reset() {
    logExporter.reset();
  }

  @Test
  void testNullStack() {
    IItem event = mock(IItem.class);
    when(eventReader.getStackTrace(event)).thenReturn(null); // just to be explicit

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getBoolean(CONFIG_KEY_TLAB_ENABLED, DEFAULT_MEMORY_ENABLED)).thenReturn(true);

    TLABProcessor processor = TLABProcessor.builder(config).eventReader(eventReader).build();
    processor.accept(event);
    // success, no NPEs
  }

  @Test
  void testProfilingDisabled() {
    IItem event =
        mock(
            IItem.class,
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                throw new IllegalStateException(
                    "IItem methods should not be called when TLAB profiling is not enabled");
              }
            });

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getBoolean(CONFIG_KEY_TLAB_ENABLED, DEFAULT_MEMORY_ENABLED)).thenReturn(false);

    TLABProcessor processor = TLABProcessor.builder(config).build();
    processor.accept(event);
  }

  @Test
  void testProcess() {
    Instant now = Instant.now();
    String stackAsString =
        "\"mockingbird\" #606\n"
            + "   java.lang.Thread.State: UNKNOWN\n"
            + "i am a serialized stack believe me";

    StackSerializer serializer = mock(StackSerializer.class);
    LogDataCommonAttributes commonAttrs = new LogDataCommonAttributes(new EventPeriods(x -> null));
    Clock clock = new MockClock(now);

    IItem event = createMockEvent(serializer, now);

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
            .eventReader(eventReader)
            .stackSerializer(serializer)
            .logEmitter(otelLogger)
            .commonAttributes(commonAttrs)
            .stackDepth(128)
            .build();

    TLABProcessor processor =
        TLABProcessor.builder(config)
            .eventReader(eventReader)
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

  private IItem createMockEvent(StackSerializer serializer, Instant now) {
    String stackAsString = "i am a serialized stack believe me";

    IItem event = mock(IItem.class);
    IMCStackTrace stack = mock(IMCStackTrace.class);
    IType eventType = mock(IType.class);
    IMCThread mockThread = mock(IMCThread.class);

    when(eventReader.getStartInstant(event)).thenReturn(now);
    when(eventReader.getStackTrace(event)).thenReturn(stack);
    when(event.getType()).thenReturn(eventType);
    when(eventReader.getAllocationSize(event)).thenReturn(ONE_MB);
    when(eventReader.getThread(event)).thenReturn(mockThread);
    when(mockThread.getThreadId()).thenReturn(THREAD_ID);
    when(mockThread.getThreadName()).thenReturn("mockingbird");
    when(stack.getTruncationState()).thenReturn(IMCStackTrace.TruncationState.NOT_TRUNCATED);
    when(eventType.getIdentifier()).thenReturn("tee-lab");
    when(serializer.serialize(stack)).thenReturn(stackAsString);

    return event;
  }

  private static final AttributeKey<String> SAMPLER_NAME_KEY =
      AttributeKey.stringKey("sampler.name");
  private static final AttributeKey<String> SAMPLER_LIMIT_KEY =
      AttributeKey.stringKey("sampler.limit");

  @Test
  void testSampling() {
    StackSerializer serializer = mock(StackSerializer.class);
    LogDataCommonAttributes commonAttrs = new LogDataCommonAttributes(new EventPeriods(x -> null));
    SpanContextualizer spanContextualizer = mock(SpanContextualizer.class);
    when(spanContextualizer.link(anyLong())).thenReturn(SpanLinkage.NONE);

    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getBoolean(CONFIG_KEY_TLAB_ENABLED, DEFAULT_MEMORY_ENABLED)).thenReturn(true);

    AllocationEventExporter allocationEventExporter =
        PlainTextAllocationEventExporter.builder()
            .eventReader(eventReader)
            .stackSerializer(serializer)
            .logEmitter(otelLogger)
            .commonAttributes(commonAttrs)
            .stackDepth(128)
            .build();

    RateLimitingAllocationEventSampler sampler = new RateLimitingAllocationEventSampler("100/s");
    TLABProcessor processor =
        new TLABProcessor.Builder(true)
            .eventReader(eventReader)
            .allocationEventExporter(allocationEventExporter)
            .spanContextualizer(spanContextualizer)
            .sampler(sampler)
            .build();

    IItem event = createMockEvent(serializer, Instant.now());

    for (int i = 0; i < 10; i++) {
      sampler.updateSampler(i % 2 == 0 ? 1.0 : 0.0);
      processor.accept(event);

      if (i % 2 == 0) {
        assertThat(logExporter.getFinishedLogItems())
            .satisfiesExactly(
                log ->
                    assertThat(log)
                        .hasAttributes(
                            entry(SOURCE_TYPE, "otel.profiling"),
                            entry(SOURCE_EVENT_NAME, "tee-lab"),
                            entry(SAMPLER_NAME_KEY, "Rate limiting sampler"),
                            entry(SAMPLER_LIMIT_KEY, "100/s"),
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
