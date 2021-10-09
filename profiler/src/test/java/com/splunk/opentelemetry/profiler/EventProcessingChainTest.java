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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.events.ContextAttached;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventProcessingChainTest {

  @Mock SpanContextualizer contextualizer;
  @Mock ThreadDumpProcessor threadDumpProcessor;
  @Mock TLABProcessor tlabProcessor;

  @Test
  void testLifecycle() {
    EventType contextAttachedType = newEventType(ContextAttached.EVENT_NAME);
    EventType threadDumpType = newEventType(ThreadDumpProcessor.EVENT_NAME);
    EventType tlabType = newEventType(TLABProcessor.NEW_TLAB_EVENT_NAME);

    Instant now = Instant.now();

    RecordedEvent tlab1 = newEvent(tlabType, null);
    RecordedEvent tlab2 = newEvent(tlabType, null);
    // context even happens after the thread dump but is seen first
    RecordedEvent contextEvent = newEvent(contextAttachedType, now);
    RecordedEvent threadDump = newEvent(threadDumpType, now.minus(250, ChronoUnit.MILLIS));

    EventProcessingChain chain =
        new EventProcessingChain(contextualizer, threadDumpProcessor, tlabProcessor);
    chain.accept(tlab1);
    verify(tlabProcessor).accept(tlab1);
    chain.accept(contextEvent);
    chain.accept(tlab2);
    verify(tlabProcessor).accept(tlab2);
    chain.accept(threadDump);

    verifyNoInteractions(threadDumpProcessor);
    verifyNoInteractions(contextualizer);

    chain.flushBuffer();
    InOrder inOrder = inOrder(threadDumpProcessor, contextualizer);
    inOrder.verify(threadDumpProcessor).accept(threadDump);
    inOrder.verify(contextualizer).updateContext(contextEvent);
  }

  @Test
  void testFlushOnChunkChange() {
    EventProcessingChain chain =
        new EventProcessingChain(contextualizer, threadDumpProcessor, tlabProcessor);

    List<RecordedEvent> events = new ArrayList<>();
    EventType contextAttached = newEventType(ContextAttached.EVENT_NAME);
    EventType threadDump = newEventType(ThreadDumpProcessor.EVENT_NAME);
    // first chunk
    for (int i = 0; i < 10; i++) {
      events.add(newEvent(contextAttached, Instant.now()));
      events.add(newEvent(threadDump, Instant.now()));
    }
    for (RecordedEvent event : events) {
      chain.accept(event);
    }

    // we have not detected end of chunk so buffer is not flushed yet
    verifyNoInteractions(threadDumpProcessor);
    verifyNoInteractions(contextualizer);

    // create new event types to simulate change of chunk
    contextAttached = newEventType(ContextAttached.EVENT_NAME);
    threadDump = newEventType(ThreadDumpProcessor.EVENT_NAME);

    // second chunk
    List<RecordedEvent> events2 = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      // context update is the first event that triggers chunk change
      events2.add(newEvent(contextAttached, Instant.now()));
      events2.add(newEvent(threadDump, Instant.now()));
    }
    for (RecordedEvent event : events2) {
      chain.accept(event);
    }

    // first chunk was flushed, second chunk is in buffer
    verify(contextualizer, times(10)).updateContext(any());
    verify(threadDumpProcessor, times(10)).accept(any());
    InOrder inOrder = inOrder(threadDumpProcessor, contextualizer);
    for (int i = 0; i < 10; i++) {
      inOrder.verify(contextualizer).updateContext(events.get(2 * i));
      inOrder.verify(threadDumpProcessor).accept(events.get(2 * i + 1));
    }

    // create new event types to simulate change of chunk
    contextAttached = newEventType(ContextAttached.EVENT_NAME);
    threadDump = newEventType(ThreadDumpProcessor.EVENT_NAME);

    // third chunk
    List<RecordedEvent> events3 = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      // thread dump is the first event that triggers chunk change
      events3.add(newEvent(threadDump, Instant.now()));
      events3.add(newEvent(contextAttached, Instant.now()));
    }
    for (RecordedEvent event : events3) {
      chain.accept(event);
    }

    // second chunk was flushed, third chunk is in buffer
    verify(contextualizer, times(20)).updateContext(any());
    verify(threadDumpProcessor, times(20)).accept(any());
    for (int i = 0; i < 10; i++) {
      inOrder.verify(contextualizer).updateContext(events2.get(2 * i));
      inOrder.verify(threadDumpProcessor).accept(events2.get(2 * i + 1));
    }

    // manually flush third chunk
    chain.flushBuffer();
    verify(contextualizer, times(30)).updateContext(any());
    verify(threadDumpProcessor, times(30)).accept(any());
    for (int i = 0; i < 10; i++) {
      inOrder.verify(threadDumpProcessor).accept(events3.get(2 * i));
      inOrder.verify(contextualizer).updateContext(events3.get(2 * i + 1));
    }
  }

  private EventType newEventType(String name) {
    EventType type = mock(EventType.class);
    when(type.getName()).thenReturn(name);
    return type;
  }

  private RecordedEvent newEvent(EventType eventType, Instant startTime) {
    RecordedEvent event = mock(RecordedEvent.class);
    when(event.getEventType()).thenReturn(eventType);
    if (startTime != null) {
      when(event.getStartTime()).thenReturn(startTime);
    }
    return event;
  }
}
