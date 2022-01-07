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
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_TYPE;
import static com.splunk.opentelemetry.profiler.TLABProcessor.ALLOCATION_SIZE_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.events.EventPeriods;
import com.splunk.opentelemetry.profiler.util.StackSerializer;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.logs.LogProcessor;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

class TLABProcessorTest {

  public static final long ONE_MB = 1024 * 1024L;
  public static final long FIVE_MB = 5 * 1024 * 1024L;

  @Test
  void testProcess() {
    Long tlabSize = FIVE_MB;
    tlabProcessTest(tlabSize);
  }

  @Test
  void testNullStack() {
    RecordedEvent event = mock(RecordedEvent.class);
    when(event.getStackTrace()).thenReturn(null); // just to be explicit

    Config config = mock(Config.class);
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

    Config config = mock(Config.class);
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
    AtomicReference<LogData> seenLogData = new AtomicReference<>();
    LogProcessor consumer = seenLogData::set;
    String stackAsString = "\"mockingbird\" #606 prio=0 os_prio=0 cpu=0ms elapsed=0s tid=0 nid=0 unknown\n" +
            "   java.lang.Thread.State: UNKNOWN\n" +
            "i am a serialized stack believe me";

    StackSerializer serializer = mock(StackSerializer.class);
    LogDataCommonAttributes commonAttrs = new LogDataCommonAttributes(new EventPeriods(x -> null));
    Clock clock = new MockClock(now);

    RecordedEvent event = createMockEvent(serializer, now, tlabSize);

    Config config = mock(Config.class);
    when(config.getBoolean(CONFIG_KEY_TLAB_ENABLED, DEFAULT_MEMORY_ENABLED)).thenReturn(true);

    TLABProcessor processor =
        TLABProcessor.builder(config)
            .stackSerializer(serializer)
            .logProcessor(consumer)
            .commonAttributes(commonAttrs)
            .resource(Resource.getDefault())
            .build();

    processor.accept(event);

    assertEquals(stackAsString, seenLogData.get().getBody().asString());
    assertEquals(
        TimeUnit.SECONDS.toNanos(now.getEpochSecond()) + clock.nanoTime(),
        seenLogData.get().getEpochNanos());
    assertEquals("otel.profiling", seenLogData.get().getAttributes().get(SOURCE_TYPE));
    assertEquals("tee-lab", seenLogData.get().getAttributes().get(SOURCE_EVENT_NAME));
    assertEquals(ONE_MB, seenLogData.get().getAttributes().get(ALLOCATION_SIZE_KEY));
    assertFalse(seenLogData.get().getSpanContext().isValid());
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
    when(mockThread.getJavaThreadId()).thenReturn(606L);
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

    AtomicReference<LogData> seenLogData = new AtomicReference<>();
    LogProcessor consumer = seenLogData::set;
    StackSerializer serializer = mock(StackSerializer.class);
    LogDataCommonAttributes commonAttrs = new LogDataCommonAttributes(new EventPeriods(x -> null));

    Config config = mock(Config.class);
    when(config.getBoolean(CONFIG_KEY_TLAB_ENABLED, DEFAULT_MEMORY_ENABLED)).thenReturn(true);
    when(config.getInt(CONFIG_KEY_MEMORY_SAMPLER_INTERVAL, DEFAULT_MEMORY_SAMPLING_INTERVAL))
        .thenReturn(samplerInterval);

    TLABProcessor processor =
        TLABProcessor.builder(config)
            .stackSerializer(serializer)
            .logProcessor(consumer)
            .commonAttributes(commonAttrs)
            .resource(Resource.getDefault())
            .build();

    RecordedEvent event = createMockEvent(serializer, Instant.now(), null);

    for (int i = 0; i < samplerInterval + 2; i++) {
      processor.accept(event);

      if (i % samplerInterval == 0) {
        assertEquals("otel.profiling", seenLogData.get().getAttributes().get(SOURCE_TYPE));
        assertEquals("tee-lab", seenLogData.get().getAttributes().get(SOURCE_EVENT_NAME));
        assertEquals("Systematic sampler", seenLogData.get().getAttributes().get(SAMPLER_NAME_KEY));
        assertEquals(
            Long.valueOf(samplerInterval),
            seenLogData.get().getAttributes().get(SAMPLER_INTERVAL_KEY));
      } else {
        assertNull(seenLogData.get());
      }
      seenLogData.set(null);
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
