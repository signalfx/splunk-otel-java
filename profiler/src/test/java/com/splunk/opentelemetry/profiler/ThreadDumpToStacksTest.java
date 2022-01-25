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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.context.TracingStacksOnlyFilter;
import com.splunk.opentelemetry.profiler.events.ContextAttached;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ThreadDumpToStacksTest {

  // Some handpicked entries of thread ID to name from threadDumpResult to test filtering
  static List<SampleThread> sampleThreadsFromDump =
      Arrays.asList(
          new SampleThread(46, "grpc-nio-worker-ELG-1-2"),
          new SampleThread(48, "grpc-nio-worker-ELG-1-3"),
          new SampleThread(56, "http-nio-9966-exec-5"));

  String threadDumpResult;

  @BeforeEach
  void setup() throws Exception {
    InputStream in =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("thread-dump1.txt");
    threadDumpResult = new String(in.readAllBytes(), StandardCharsets.UTF_8);
  }

  @Test
  void testStream() {
    ThreadDumpToStacks threadDumpToStacks = new ThreadDumpToStacks(new StackTraceFilter(false));
    Stream<String> resultStream = threadDumpToStacks.toStream(threadDumpResult);
    List<String> result = resultStream.collect(Collectors.toList());
    assertEquals(28, result.size());
    Stream.of(StackTraceFilter.UNWANTED_PREFIXES)
        .forEach(
            prefix -> {
              assertThat(result).noneMatch(stack -> stack.contains(prefix));
            });
  }

  @Test
  void testFilterStacksWithoutSpans() {
    SpanContextualizer contextualizer = new SpanContextualizer();
    sampleThreadsFromDump.forEach(
        it -> contextualizer.updateContext(threadContextStartEvent(it.threadId)));

    ThreadDumpToStacks threadDumpToStacks =
        new ThreadDumpToStacks(
            new TracingStacksOnlyFilter(
                (wallOfStacks, startIndex, lastIndex) -> true, contextualizer));

    Stream<String> resultStream = threadDumpToStacks.toStream(threadDumpResult);
    List<String> result = resultStream.collect(Collectors.toList());
    assertEquals(sampleThreadsFromDump.size(), result.size());

    sampleThreadsFromDump.forEach(
        sample -> assertThat(result).anyMatch(stack -> stack.contains(sample.threadName)));
  }

  @Test
  void edgeCase1_simplyHitsEnd() {
    StackTraceFilter filter = mock(StackTraceFilter.class);
    when(filter.test(isA(String.class), anyInt(), anyInt())).thenReturn(true);

    ThreadDumpToStacks threadDumpToStacks = new ThreadDumpToStacks(filter);
    Stream<String> resultStream = threadDumpToStacks.toStream("something\n\n");
    List<String> result = resultStream.collect(Collectors.toList());
    assertThat(result).containsExactly("something");
  }

  @Test
  void edgeCase2_emptyString() {
    ThreadDumpToStacks threadDumpToStacks = new ThreadDumpToStacks(null);
    Stream<String> resultStream = threadDumpToStacks.toStream("");
    List<String> result = resultStream.collect(Collectors.toList());
    assertTrue(result.isEmpty());
  }

  private static RecordedEvent threadContextStartEvent(long threadId) {
    RecordedEvent event = mock(RecordedEvent.class);
    EventType type = mock(EventType.class);
    when(type.getName()).thenReturn(ContextAttached.EVENT_NAME);
    when(event.getEventType()).thenReturn(type);
    when(event.getString("traceId")).thenReturn("deadbeefdeadbeefdeadbeefdeadbeef");
    when(event.getString("spanId")).thenReturn("0123012301230123");
    RecordedThread thread = mock(RecordedThread.class);
    when(thread.getJavaThreadId()).thenReturn(threadId);
    when(event.getThread()).thenReturn(thread);
    return event;
  }

  static class SampleThread {
    final long threadId;
    final String threadName;

    SampleThread(long threadId, String threadName) {
      this.threadId = threadId;
      this.threadName = threadName;
    }
  }
}
