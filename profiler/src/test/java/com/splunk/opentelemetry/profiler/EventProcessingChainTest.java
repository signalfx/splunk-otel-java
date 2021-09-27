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

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.events.ContextAttached;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventProcessingChainTest {

  @Mock RecordedEvent tlab1;
  @Mock RecordedEvent tlab2;
  @Mock RecordedEvent contextEvent;
  @Mock RecordedEvent threadDump;

  @Mock SpanContextualizer contextualizer;
  @Mock ThreadDumpProcessor threadDumpProcessor;
  @Mock TLABProcessor tlabProcessor;

  @BeforeEach
  void setup() {
    setType(tlab1, TLABProcessor.NEW_TLAB_EVENT_NAME);
    setType(tlab2, TLABProcessor.NEW_TLAB_EVENT_NAME);
    setType(contextEvent, ContextAttached.EVENT_NAME);
    setType(threadDump, ThreadDumpProcessor.EVENT_NAME);
  }

  @Test
  void testLifecycle() {
    Instant now = Instant.now();
    // context even happens after the thread dump but is seen first
    when(contextEvent.getStartTime()).thenReturn(now);
    when(threadDump.getStartTime()).thenReturn(now.minus(250, ChronoUnit.MILLIS));
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

  private void setType(RecordedEvent event, String name) {
    EventType type = mock(EventType.class);
    when(type.getName()).thenReturn(name);
    when(event.getEventType()).thenReturn(type);
  }
}
