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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.Test;

class ThreadContextTrackerTest {

  final long threadId = 21L;
  final String traceId = "9999-99--9999999-9-9-9";

  @Test
  void testDefaultStateNoLinkFound() {
    ThreadContextTracker tracker = new ThreadContextTracker();
    List<SpanLinkage> inFlight = tracker.getInFlightSpansForThread(threadId);
    assertTrue(inFlight.isEmpty());
  }

  @Test
  void testSingleSpanLinkedAndFound() {
    String spanId = "abc-123-fgggg";
    SpanLinkage linkage = makeLinkage(spanId, threadId);

    ThreadContextTracker tracker = new ThreadContextTracker();
    tracker.addLinkage(linkage);
    List<SpanLinkage> inFlight = tracker.getInFlightSpansForThread(threadId);

    assertEquals(1, inFlight.size());
    assertEquals(traceId, inFlight.get(0).getTraceId());
    assertEquals(threadId, inFlight.get(0).getThreadId());
    assertEquals(spanId, inFlight.get(0).getSpanId());
  }

  @Test
  void testAddAndRemoveWithMultipleFound() {
    ThreadContextTracker tracker = new ThreadContextTracker();
    tracker.addLinkage(makeLinkage("abc1", 12));
    tracker.addLinkage(makeLinkage("abc2", 13));
    tracker.addLinkage(makeLinkage("abc3", 12));
    tracker.unlink(traceId, "abc2", 13);
    tracker.addLinkage(makeLinkage("abc3", 12));
    tracker.unlink(traceId, "abc3", 12);
    tracker.addLinkage(makeLinkage("abc9", 13));
    tracker.addLinkage(makeLinkage("abc4", 14));
    tracker.addLinkage(makeLinkage("abc5", 12));

    assertEquals(3, tracker.getInFlightSpansForThread(12).size());
    assertEquals(1, tracker.getInFlightSpansForThread(13).size());
    assertEquals(1, tracker.getInFlightSpansForThread(14).size());
  }

  private SpanLinkage makeLinkage(String spanId, long threadId) {
    RecordedThread thread = mock(RecordedThread.class);
    RecordedEvent event = mock(RecordedEvent.class);
    when(event.getThread()).thenReturn(thread);
    when(thread.getJavaThreadId()).thenReturn(threadId);
    return new SpanLinkage(traceId, spanId, event);
  }
}
