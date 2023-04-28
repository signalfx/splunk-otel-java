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

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.allocation.exporter.AllocationEventExporter;
import com.splunk.opentelemetry.profiler.allocation.sampler.RateLimitingAllocationEventSampler;
import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.events.ContextAttached;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventProcessingChainTest {

  @Mock EventReader eventReader;
  @Mock SpanContextualizer contextualizer;
  @Mock ThreadDumpProcessor threadDumpProcessor;
  @Mock TLABProcessor tlabProcessor;

  @Test
  void testLifecycle() {
    IType<?> contextAttachedType = newEventType(ContextAttached.EVENT_NAME);
    IType<?> threadDumpType = newEventType(ThreadDumpProcessor.EVENT_NAME);
    IType<?> tlabType = newEventType(TLABProcessor.NEW_TLAB_EVENT_NAME);

    Instant now = Instant.now();

    IItem tlab1 = newEvent(tlabType, now.minus(100, ChronoUnit.MILLIS));
    IItem tlab2 = newEvent(tlabType, now.plus(100, ChronoUnit.MILLIS));
    // context even happens after the thread dump but is seen first
    IItem contextEvent = newEvent(contextAttachedType, now);
    IItem threadDump = newEvent(threadDumpType, now.minus(250, ChronoUnit.MILLIS));

    EventProcessingChain chain =
        new EventProcessingChain(eventReader, contextualizer, threadDumpProcessor, tlabProcessor);
    chain.accept(tlab1);
    chain.accept(contextEvent);
    chain.accept(tlab2);
    chain.accept(threadDump);

    verifyNoInteractions(contextualizer, threadDumpProcessor, tlabProcessor);

    chain.flushBuffer();
    InOrder inOrder = inOrder(contextualizer, threadDumpProcessor, tlabProcessor);
    inOrder.verify(threadDumpProcessor).accept(threadDump);
    inOrder.verify(tlabProcessor).accept(tlab1);
    inOrder.verify(contextualizer).updateContext(contextEvent);
    inOrder.verify(tlabProcessor).accept(tlab2);
    inOrder.verify(tlabProcessor).flush();
    inOrder.verify(threadDumpProcessor).flush();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void eventsDispatchedInTimeOrder() {
    IType<?> contextAttachedType = newEventType(ContextAttached.EVENT_NAME);
    IType<?> threadDumpType = newEventType(ThreadDumpProcessor.EVENT_NAME);
    Instant now = Instant.now();
    IItem event1 = newEvent(contextAttachedType, now.plus(1, SECONDS));
    IItem event2 = newEvent(contextAttachedType, now.plus(2, SECONDS));
    IItem event3 = newEvent(threadDumpType, now.plus(3, SECONDS));

    // Our events are in time order: context1, context2, threadDump1, threadDump2.
    // However, we will send them to the chain out of order: context2, context1, threadDump.
    // Expectation is that we see them dispatched in the correct order.

    EventProcessingChain chain =
        new EventProcessingChain(eventReader, contextualizer, threadDumpProcessor, tlabProcessor);
    chain.accept(event2); // Out of order
    chain.accept(event1); // Out of order
    chain.accept(event3);
    chain.flushBuffer();

    InOrder ordered = inOrder(contextualizer, threadDumpProcessor);
    ordered.verify(contextualizer).updateContext(event1);
    ordered.verify(contextualizer).updateContext(event2);
    ordered.verify(threadDumpProcessor).accept(event3);
    ordered.verify(threadDumpProcessor).flush();
    ordered.verifyNoMoreInteractions();
  }

  @Test
  void eventRateLimit() {
    IType<?> eventType = newEventType(TLABProcessor.NEW_TLAB_EVENT_NAME);
    Instant now = Instant.now();
    List<IItem> events = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      IItem event = newEvent(eventType, now.plus(i, MILLIS));
      when(eventReader.getStackTrace(event)).thenReturn(mock(IMCStackTrace.class));
      events.add(event);
    }

    RateLimitingAllocationEventSampler sampler = new RateLimitingAllocationEventSampler("100/s");
    AtomicInteger receivedCount = new AtomicInteger();
    AllocationEventExporter exporter =
        (event, sampler1, spanContext) -> receivedCount.incrementAndGet();
    SpanContextualizer spanContextualizer = mock(SpanContextualizer.class);

    TLABProcessor processor =
        new TLABProcessor.Builder(true)
            .eventReader(eventReader)
            .allocationEventExporter(exporter)
            .spanContextualizer(spanContextualizer)
            .sampler(sampler)
            .build();
    EventProcessingChain chain =
        new EventProcessingChain(eventReader, contextualizer, threadDumpProcessor, processor);
    for (IItem event : events) {
      chain.accept(event);
    }
    chain.flushBuffer();

    // probabilistic sampling can over or undersample
    assertThat(receivedCount.get()).isCloseTo(100, Offset.offset(30));
    assertThat(sampler.maxEventsPerSecond()).isEqualTo(100);
  }

  private IType<?> newEventType(String name) {
    IType<?> type = mock(IType.class);
    when(type.getIdentifier()).thenReturn(name);
    return type;
  }

  private IItem newEvent(IType<?> eventType, Instant startTime) {
    IItem event = mock(IItem.class);
    if (eventType != null) {
      when(event.getType()).thenReturn((IType) eventType);
    }
    if (startTime != null) {
      when(eventReader.getStartInstant(event)).thenReturn(startTime);
      when(eventReader.getStartTime(event))
          .thenReturn(TimeUnit.SECONDS.toNanos(startTime.getEpochSecond()) + startTime.getNano());
    }
    return event;
  }
}
