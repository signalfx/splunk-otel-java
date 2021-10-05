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

import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_EVENT_NAME;
import static com.splunk.opentelemetry.profiler.ProfilingSemanticAttributes.SOURCE_TYPE;
import static com.splunk.opentelemetry.profiler.TLABProcessor.ALLOCATION_SIZE_KEY;
import static com.splunk.opentelemetry.profiler.TLABProcessor.TLAB_SIZE_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.logs.LogEntry;
import com.splunk.opentelemetry.logs.LogsProcessor;
import com.splunk.opentelemetry.profiler.events.EventPeriods;
import com.splunk.opentelemetry.profiler.util.StackSerializer;
import io.opentelemetry.instrumentation.api.config.Config;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;
import org.junit.jupiter.api.Test;

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
    when(config.getBoolean(Configuration.CONFIG_KEY_TLAB_ENABLED)).thenReturn(true);

    TLABProcessor processor = new TLABProcessor(config, null, null, null);
    processor.accept(event);
    // success, no NPEs
  }

  @Test
  void testAllocationOutsideTLAB() {
    tlabProcessTest(null);
  }

  private void tlabProcessTest(Long tlabSize) {
    Instant now = Instant.now();
    AtomicReference<LogEntry> seenLogEntry = new AtomicReference<>();
    LogsProcessor consumer = seenLogEntry::set;
    String stackAsString = "i am a serialized stack believe me";

    RecordedEvent event = mock(RecordedEvent.class);
    StackSerializer serializer = mock(StackSerializer.class);
    RecordedStackTrace stack = mock(RecordedStackTrace.class);
    EventType eventType = mock(EventType.class);
    LogEntryCommonAttributes commonAttrs =
        new LogEntryCommonAttributes(new EventPeriods(x -> null));

    when(event.getStartTime()).thenReturn(now);
    when(event.getStackTrace()).thenReturn(stack);
    when(event.getEventType()).thenReturn(eventType);
    when(event.getLong("allocationSize")).thenReturn(ONE_MB);

    when(event.hasField("tlabSize")).thenReturn(tlabSize != null);
    if (tlabSize == null) {
      when(event.getLong("tlabSize")).thenThrow(NullPointerException.class);
    } else {
      when(event.getLong("tlabSize")).thenReturn(tlabSize);
    }
    when(eventType.getName()).thenReturn("tee-lab");
    when(serializer.serialize(stack)).thenReturn(stackAsString);

    Config config = mock(Config.class);
    when(config.getBoolean(Configuration.CONFIG_KEY_TLAB_ENABLED)).thenReturn(true);

    TLABProcessor processor = new TLABProcessor(config, serializer, consumer, commonAttrs);
    processor.accept(event);

    assertEquals(stackAsString, seenLogEntry.get().getBody());
    assertEquals(now, seenLogEntry.get().getTime());
    assertEquals("otel.profiling", seenLogEntry.get().getAttributes().get(SOURCE_TYPE));
    assertEquals("tee-lab", seenLogEntry.get().getAttributes().get(SOURCE_EVENT_NAME));
    assertEquals(ONE_MB, seenLogEntry.get().getAttributes().get(ALLOCATION_SIZE_KEY));
    if (tlabSize == null) {
      assertNull(seenLogEntry.get().getAttributes().get(TLAB_SIZE_KEY));
    } else {
      assertEquals(tlabSize, seenLogEntry.get().getAttributes().get(TLAB_SIZE_KEY));
    }
    assertNull(seenLogEntry.get().getSpanId());
    assertNull(seenLogEntry.get().getTraceId());
  }
}
