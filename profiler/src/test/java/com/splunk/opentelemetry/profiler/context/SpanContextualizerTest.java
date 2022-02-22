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

package com.splunk.opentelemetry.profiler.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.ThreadDumpRegion;
import com.splunk.opentelemetry.profiler.events.ContextAttached;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.Test;

class SpanContextualizerTest {

  private final String traceId = "deadbeefdeadbeefdeadbeefdeadbeef";
  private final String spanId = "0123012301230123";
  private final String rawStack = "raw is raw";

  @Test
  void testSimplePath() {
    SpanContextualizer testClass = new SpanContextualizer();

    Events events = buildEvents(spanId, 906);

    assertNoLinkage(testClass, events);
    testClass.updateContext(events.scopeStart);
    assertLinkage(testClass, events);
    testClass.updateContext(events.scopeEnd);
    assertNoLinkage(testClass, events);
    testClass.updateContext(events.scopeEnd);
    assertNoLinkage(testClass, events);
  }

  @Test
  void testNestedChildSpanOneThread() {
    SpanContextualizer testClass = new SpanContextualizer();

    String childSpanId = "0123012301230123";
    String rawStack1 = "raw is raw1";
    String rawStack2 = "raw is raw2";
    long threadId = 906;

    Events parent = buildEvents(spanId, threadId);
    Events child = buildEvents(childSpanId, threadId, parent);

    testClass.updateContext(parent.scopeStart);
    assertLinkage(testClass, parent, rawStack1);

    testClass.updateContext(child.scopeStart);
    assertLinkage(testClass, child, rawStack2);

    testClass.updateContext(child.scopeEnd);
    assertLinkage(testClass, parent, rawStack1);

    testClass.updateContext(parent.scopeEnd);
    assertNoLinkage(testClass, parent);
  }

  @Test
  void testOneSpanThreadHops() {
    SpanContextualizer testClass = new SpanContextualizer();

    Events events1 = buildEvents(spanId, 906);
    Events events2 = buildEvents(spanId, 907);

    testClass.updateContext(events1.scopeStart); // has some scope on the first thread
    testClass.updateContext(events2.scopeStart); // context switch to a new thread

    assertLinkage(testClass, events1);
    assertLinkage(testClass, events2);

    testClass.updateContext(events2.scopeEnd);

    assertNoLinkage(testClass, events2);
    assertLinkage(testClass, events1); // thread 1 is still linked
  }

  @Test
  void testMultipleThreadsOneSpanAllLinked() {
    SpanContextualizer testClass = new SpanContextualizer();

    long threadId1 = 907;
    long threadId2 = 908;
    long threadId3 = 909;
    Events events1 = buildEvents(spanId, threadId1);
    Events events2 = buildEvents(spanId, threadId2);
    Events events3 = buildEvents(spanId, threadId3);

    testClass.updateContext(events1.scopeStart); // context switch to a new thread
    assertLinkage(testClass, events1);
    assertNoLinkage(testClass, events2);
    assertNoLinkage(testClass, events3);

    testClass.updateContext(events2.scopeStart); // context switch to a new thread
    assertLinkage(testClass, events1);
    assertLinkage(testClass, events2);
    assertNoLinkage(testClass, events3);

    testClass.updateContext(events3.scopeStart); // context switch to a new thread
    assertLinkage(testClass, events1);
    assertLinkage(testClass, events2);
    assertLinkage(testClass, events3);

    testClass.updateContext(events2.scopeEnd);
    assertLinkage(testClass, events1);
    assertNoLinkage(testClass, events2);
    assertLinkage(testClass, events3);

    testClass.updateContext(events3.scopeEnd);
    assertLinkage(testClass, events1);
    assertNoLinkage(testClass, events2);
    assertNoLinkage(testClass, events3);

    testClass.updateContext(events1.scopeEnd);
    assertNoLinkage(testClass, events1);
    assertNoLinkage(testClass, events2);
    assertNoLinkage(testClass, events3);
  }

  @Test
  void testBackgroundWorkerThreads() {
    SpanContextualizer testClass = new SpanContextualizer();

    Events events1 = buildEvents(spanId, 906);
    Events events2 = buildEvents(spanId, 907);
    Events events3 = buildEvents(spanId, 908);

    testClass.updateContext(events1.scopeStart);
    testClass.updateContext(events2.scopeStart);
    testClass.updateContext(events3.scopeStart);

    // All are linked
    assertLinkage(testClass, events1);
    assertLinkage(testClass, events2);
    assertLinkage(testClass, events3);

    testClass.updateContext(events1.scopeEnd);

    // One is no longer linked, but 2 and 3 are
    assertNoLinkage(testClass, events1);
    assertLinkage(testClass, events2);
    assertLinkage(testClass, events3);

    testClass.updateContext(events3.scopeEnd);
    assertNoLinkage(testClass, events1);
    assertLinkage(testClass, events2);
    assertNoLinkage(testClass, events3);

    testClass.updateContext(events2.scopeEnd);
    assertNoLinkage(testClass, events1);
    assertNoLinkage(testClass, events2);
    assertNoLinkage(testClass, events3);

    assertEquals(0, testClass.inFlightThreadCount());
  }

  @Test
  void testMultipleChildThreadedSpansEndWithinParent() {
    SpanContextualizer testClass = new SpanContextualizer();

    Events events1 = buildEvents("0123012301230123", 906);
    Events events2 = buildEvents("9999888899998888", 907);
    Events events3 = buildEvents("5555666655556666", 908);

    testClass.updateContext(events1.scopeStart);
    testClass.updateContext(events2.scopeStart);
    testClass.updateContext(events3.scopeStart);

    assertLinkage(testClass, events1);
    assertLinkage(testClass, events2);
    assertLinkage(testClass, events3);

    // 3 is ending before 2, even tho it was started after
    testClass.updateContext(events3.scopeEnd);
    assertLinkage(testClass, events1);
    assertLinkage(testClass, events1);
    assertNoLinkage(testClass, events3);
    assertLinkage(testClass, events2);

    // Now 2 is ending
    testClass.updateContext(events2.scopeEnd);
    assertLinkage(testClass, events1);
    assertLinkage(testClass, events1);
    assertNoLinkage(testClass, events2);
    assertNoLinkage(testClass, events3);

    testClass.updateContext(events1.scopeEnd);
    assertNoLinkage(testClass, events1);

    // TODO: Verify size?
  }

  @Test
  void testOneSpanThreadHop_FirstThreadClosesFirst() {
    SpanContextualizer testClass = new SpanContextualizer();
    Events events1 = buildEvents(spanId, 906);
    Events events2 = buildEvents(spanId, 907);
    testClass.updateContext(events1.scopeStart);
    testClass.updateContext(events2.scopeStart); //  hop to new thread

    assertLinkage(testClass, events1); // thread 1 is linked
    assertLinkage(testClass, events2); // thread 2 is also linked

    testClass.updateContext(events1.scopeEnd); // thread 1 closes

    assertNoLinkage(testClass, events1); // thread 1 no longer linked
    assertLinkage(testClass, events2); // thread 2 still linked

    testClass.updateContext(events2.scopeEnd);
    assertNoLinkage(testClass, events1); // thread 1 no longer linked
    assertNoLinkage(testClass, events2); // thread 2 no longer linked
  }

  @Test
  void testVeryLargeNumberOfDynamicThreads() {
    SpanContextualizer testClass = new SpanContextualizer();
    Random rand = new Random();
    int num = 500;
    Events rootEvents = buildEvents(spanId, 906);
    testClass.updateContext(rootEvents.scopeStart);
    List<Events> openEventThreads = new ArrayList<>();

    IntStream.range(0, num)
        .forEach(
            i -> {
              long threadId = rootEvents.threadId + i + 1;
              Events ev = buildEvents(spanId, threadId);
              testClass.updateContext(ev.scopeStart);
              if (rand.nextInt(100) > 50) { // sometimes nested, sometimes hopping
                testClass.updateContext(ev.scopeEnd);
              } else {
                openEventThreads.add(ev);
              }
            });

    assertLinkage(testClass, rootEvents);
    Collections.shuffle(openEventThreads);

    // Close all that are still open
    openEventThreads.forEach(ev -> testClass.updateContext(ev.scopeEnd));
    openEventThreads.forEach(ev -> assertNoLinkage(testClass, ev));

    testClass.updateContext(rootEvents.scopeEnd);
    assertNoLinkage(testClass, rootEvents);
    assertEquals(0, testClass.inFlightThreadCount());
  }

  private void assertLinkage(SpanContextualizer testClass, Events events) {
    assertLinkage(testClass, events, rawStack);
  }

  private void assertLinkage(SpanContextualizer testClass, Events events, String stackBody) {
    String stack = assembleParseableStack(stackBody, events.threadId);
    ThreadDumpRegion region = new ThreadDumpRegion(stack, 0, stack.length());

    SpanLinkage result = testClass.link(region);
    assertEquals(events.spanId, result.getSpanContext().getSpanId());
    assertEquals(traceId, result.getSpanContext().getTraceId());

    SpanLinkage resultFromThread = testClass.link(result.getThreadId());
    assertEquals(events.spanId, resultFromThread.getSpanContext().getSpanId());
    assertEquals(traceId, resultFromThread.getSpanContext().getTraceId());
  }

  private void assertNoLinkage(SpanContextualizer testClass, Events events) {
    String stack = assembleParseableStack(rawStack, events.threadId);
    ThreadDumpRegion region = new ThreadDumpRegion(stack, 0, stack.length());

    assertFalse(testClass.link(region).getSpanContext().isValid());
    assertFalse(testClass.link(events.threadId).getSpanContext().isValid());
  }

  private String assembleParseableStack(String stackBody, long threadId) {
    return "\"pool-thread-1\" #" + threadId + " daemon\n" + stackBody + "\n";
  }

  private Events buildEvents(String spanId, long threadId) {
    return buildEvents(spanId, threadId, null);
  }

  private Events buildEvents(String spanId, long threadId, Events parent) {
    Events result = new Events(spanId, threadId);
    result.scopeStart = contextEventIn(spanId, threadId);
    result.scopeEnd = contextEventOut(parent, threadId);
    result.sourceEvent = mock(RecordedEvent.class);
    EventType eventType = mock(EventType.class);
    when(result.sourceEvent.getEventType()).thenReturn(eventType);
    when(eventType.getName()).thenReturn("GreatSourceEventHere");
    return result;
  }

  static class Events {
    private final String spanId;
    private final long threadId;
    public RecordedEvent scopeStart;
    public RecordedEvent scopeEnd;
    public RecordedEvent sourceEvent;

    Events(String spanId, long threadId) {
      this.spanId = spanId;
      this.threadId = threadId;
    }
  }

  private RecordedEvent contextEventIn(String spanId, long threadId) {
    return contextEvent(traceId, spanId, threadId);
  }

  private RecordedEvent contextEventOut(Events parent, long threadId) {
    if (parent == null) {
      return contextEvent(null, null, threadId);
    } else {
      return contextEvent(traceId, parent.spanId, threadId);
    }
  }

  private static RecordedEvent contextEvent(String traceId, String spanId, long threadId) {
    RecordedEvent event = mock(RecordedEvent.class);
    EventType type = mock(EventType.class);
    when(type.getName()).thenReturn(ContextAttached.EVENT_NAME);
    when(event.getEventType()).thenReturn(type);
    when(event.getString("traceId")).thenReturn(traceId);
    when(event.getString("spanId")).thenReturn(spanId);
    RecordedThread thread = mock(RecordedThread.class);
    when(thread.getJavaThreadId()).thenReturn(threadId);
    when(event.getThread()).thenReturn(thread);
    return event;
  }
}
