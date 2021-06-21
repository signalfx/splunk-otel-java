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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.events.ContextAttached;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventProcessingChainTest {

  @Mock RecordedEvent event;
  @Mock SpanContextualizer contextualizer;
  @Mock ThreadDumpProcessor threadDumpProcessor;
  @Mock EventType eventType;

  @Test
  void testContextEvent() {

    when(event.getEventType()).thenReturn(eventType);
    when(eventType.getName()).thenReturn(ContextAttached.EVENT_NAME);

    EventProcessingChain chain = new EventProcessingChain(contextualizer, threadDumpProcessor);
    chain.accept(event);
    verify(contextualizer).updateContext(event);
    verifyNoMoreInteractions(threadDumpProcessor);
  }

  @Test
  void testThreadDumpEvent() {

    when(event.getEventType()).thenReturn(eventType);
    when(eventType.getName()).thenReturn(ThreadDumpProcessor.EVENT_NAME);

    EventProcessingChain chain = new EventProcessingChain(contextualizer, threadDumpProcessor);
    chain.accept(event);
    verify(threadDumpProcessor).accept(event);
    verifyNoMoreInteractions(contextualizer);
  }
}
