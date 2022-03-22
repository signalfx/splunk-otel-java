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

import static com.splunk.opentelemetry.profiler.ThreadDumpRegionTest.readDumpFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import com.splunk.opentelemetry.profiler.events.ContextAttached;
import com.splunk.opentelemetry.profiler.exporter.ProfilingEventExporter;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.Test;

class ThreadDumpProcessorTest {

  static String traceId = "deadbeefdeadbeefdeadbeefdeadbeef";
  static String spanId = "0123012301230123";
  static byte traceFlags = TraceFlags.getSampled().asByte();

  // Some handpicked entries of thread ID to name from threadDumpResult to test filtering
  static List<SampleThread> sampleThreadsFromDump =
      Arrays.asList(
          new SampleThread(46, "grpc-nio-worker-ELG-1-2"),
          new SampleThread(48, "grpc-nio-worker-ELG-1-3"),
          new SampleThread(56, "http-nio-9966-exec-5"));

  @Test
  void testProcessEvent() {
    SpanContextualizer contextualizer = new SpanContextualizer();
    long idOfThreadRunningTheSpan = 3L;
    SpanContext expectedContext =
        SpanContext.create(
            traceId, spanId, TraceFlags.fromByte(traceFlags), TraceState.getDefault());

    contextualizer.updateContext(threadContextStartEvent(idOfThreadRunningTheSpan));

    String threadDump = readDumpFromResource("thread-dump2.txt");
    List<StackToSpanLinkage> results = collectResults(contextualizer, threadDump, false);

    assertEquals(3, results.size());

    assertFalse(results.get(0).hasSpanInfo());
    assertTrue(
        results.get(0).getRawStack().contains("at java.lang.ref.Reference$ReferenceHandler"));

    assertTrue(results.get(1).hasSpanInfo());
    assertEquals(expectedContext, results.get(1).getSpanContext());
    assertEquals(idOfThreadRunningTheSpan, results.get(1).getSpanStartThread());
    assertTrue(results.get(1).getRawStack().contains("AwesomeThinger.overHereDoingSpanThings"));

    assertFalse(results.get(2).hasSpanInfo());
    assertTrue(results.get(2).getRawStack().contains("0x0000000625152778"));
  }

  @Test
  void testFilterInternalStacks() {
    SpanContextualizer contextualizer = new SpanContextualizer();

    String threadDump = readDumpFromResource("thread-dump1.txt");
    List<StackToSpanLinkage> results = collectResults(contextualizer, threadDump, false);

    assertEquals(28, results.size());

    Stream.of(StackTraceFilter.UNWANTED_PREFIXES)
        .forEach(
            prefix -> {
              assertThat(results).noneMatch(stack -> stack.getRawStack().contains(prefix));
            });
  }

  @Test
  void testFilterStacksWithoutSpans() {
    SpanContextualizer contextualizer = new SpanContextualizer();

    sampleThreadsFromDump.forEach(
        it -> contextualizer.updateContext(threadContextStartEvent(it.threadId)));

    String threadDump = readDumpFromResource("thread-dump1.txt");
    List<StackToSpanLinkage> results = collectResults(contextualizer, threadDump, true);

    assertEquals(sampleThreadsFromDump.size(), results.size());

    sampleThreadsFromDump.forEach(
        sample ->
            assertThat(results).anyMatch(stack -> stack.getRawStack().contains(sample.threadName)));
  }

  private static RecordedEvent threadContextStartEvent(long threadId) {
    RecordedEvent event = mock(RecordedEvent.class);
    EventType type = mock(EventType.class);
    when(type.getName()).thenReturn(ContextAttached.EVENT_NAME);
    when(event.getEventType()).thenReturn(type);
    when(event.getString("traceId")).thenReturn(traceId);
    when(event.getString("spanId")).thenReturn(spanId);
    when(event.getByte("traceFlags")).thenReturn(traceFlags);
    RecordedThread thread = mock(RecordedThread.class);
    when(thread.getJavaThreadId()).thenReturn(threadId);
    when(event.getThread()).thenReturn(thread);
    return event;
  }

  private static List<StackToSpanLinkage> collectResults(
      SpanContextualizer contextualizer, String threadDump, boolean onlyTracingSpans) {
    List<StackToSpanLinkage> results = new ArrayList<>();
    ProfilingEventExporter profilingEventExporter = results::add;
    ThreadDumpProcessor processor =
        ThreadDumpProcessor.builder()
            .spanContextualizer(contextualizer)
            .profilingEventExporter(profilingEventExporter)
            .stackTraceFilter(new StackTraceFilter(false))
            .onlyTracingSpans(onlyTracingSpans)
            .build();

    RecordedEvent event = mock(RecordedEvent.class);
    EventType eventType = mock(EventType.class);
    when(event.getEventType()).thenReturn(eventType);
    when(eventType.getName()).thenReturn(ThreadDumpProcessor.EVENT_NAME);
    when(event.getString("result")).thenReturn(threadDump);

    processor.accept(event);
    return results;
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
