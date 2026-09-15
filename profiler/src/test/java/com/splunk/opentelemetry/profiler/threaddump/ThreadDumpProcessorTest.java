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

package com.splunk.opentelemetry.profiler.threaddump;

import static com.splunk.opentelemetry.profiler.threaddump.StackTraceParserTest.readDumpFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.EventReader;
import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import com.splunk.opentelemetry.profiler.events.ContextAttached;
import com.splunk.opentelemetry.profiler.exporter.CpuEventExporter;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ThreadDumpProcessorTest {

  static String traceId = "deadbeefdeadbeefdeadbeefdeadbeef";
  static String spanId = "0123012301230123";
  static byte traceFlags = TraceFlags.getSampled().asByte();

  @Mock EventReader eventReader;

  // Some handpicked entries of thread ID to name from threadDumpResult to test filtering
  static List<SampleThread> sampleThreadsFromDump =
      Arrays.asList(
          new SampleThread(46, "grpc-nio-worker-ELG-1-2"),
          new SampleThread(48, "grpc-nio-worker-ELG-1-3"),
          new SampleThread(56, "http-nio-9966-exec-5"));

  @Test
  void testProcessEvent() {
    SpanContextualizer contextualizer = new SpanContextualizer(eventReader);
    long idOfThreadRunningTheSpan = 3L;
    SpanContext expectedContext =
        SpanContext.create(
            traceId, spanId, TraceFlags.fromByte(traceFlags), TraceState.getDefault());

    contextualizer.updateContext(threadContextStartEvent(idOfThreadRunningTheSpan));

    String threadDump = readDumpFromResource("thread-dump2.txt");
    List<StackToSpanLinkage> results = collectResults(contextualizer, threadDump, false, false);

    assertEquals(3, results.size());

    assertFalse(results.get(0).hasSpanInfo());
    StackTraceData.StackTraceLine stackTraceLine =
        results.get(0).getStackTrace().getStackTraceLines().getLast();
    assertEquals("java.lang.ref.Reference$ReferenceHandler", stackTraceLine.getClassName());
    assertEquals("run", stackTraceLine.getMethod());

    assertTrue(results.get(1).hasSpanInfo());
    assertEquals(expectedContext, results.get(1).getSpanContext());
    assertEquals(idOfThreadRunningTheSpan, results.get(1).getSpanStartThread());
    stackTraceLine = results.get(1).getStackTrace().getStackTraceLines().getLast();
    assertEquals("com.something.something.AwesomeThinger", stackTraceLine.getClassName());
    assertEquals("overHereDoingSpanThings", stackTraceLine.getMethod());

    assertFalse(results.get(2).hasSpanInfo());
    assertNull(results.get(2).getStackTrace().getThreadLockData().getWaitingOn());
    assertTrue(results.get(2).getStackTrace().getThreadLockData().getLockedMonitors().isEmpty());
  }

  @Test
  void testFilterInternalStacks() {
    SpanContextualizer contextualizer = new SpanContextualizer(new EventReader());

    String threadDump = readDumpFromResource("thread-dump1.txt");
    List<StackToSpanLinkage> results = collectResults(contextualizer, threadDump, false, false);

    assertEquals(27, results.size());

    Stream.of(StackTraceFilter.UNWANTED_PREFIXES)
        .map(prefix -> prefix.endsWith("\"")
            ? prefix.substring(1, prefix.length() - 1)
            : prefix.substring(1))
        .forEach(
            prefix -> {
              assertThat(results)
                  .noneMatch(
                      stack -> stack.getStackTrace().getThreadName().startsWith(prefix));
            });
  }

  @Test
  void testFilterStacksWithoutSpans() {
    SpanContextualizer contextualizer = new SpanContextualizer(eventReader);

    sampleThreadsFromDump.forEach(
        it -> contextualizer.updateContext(threadContextStartEvent(it.threadId)));

    String threadDump = readDumpFromResource("thread-dump1.txt");
    List<StackToSpanLinkage> results = collectResults(contextualizer, threadDump, true, false);

    assertEquals(sampleThreadsFromDump.size(), results.size());

    sampleThreadsFromDump.forEach(
        sample ->
            assertThat(results)
                .anyMatch(
                    stack -> stack.getStackTrace().getThreadName().equals(sample.threadName)));
  }

  @Test
  void shouldUseConfiguredStackDepth() {
    SpanContextualizer contextualizer = new SpanContextualizer(eventReader);

    List<StackToSpanLinkage> results =
        collectResults(contextualizer, readDumpFromResource("thread-dump2.txt"), false, false, 1);

    assertEquals(1, results.getFirst().getStackTrace().getStackTraceLines().size());
    assertTrue(results.getFirst().getStackTrace().isTruncated());
  }

  @Test
  void testBuildLockToOwningThreadMapping() {
    String stackText =
        "\"holder\" #1 daemon\n"
            + "   java.lang.Thread.State: RUNNABLE\n"
            + "        - locked <0x0000000000000011> (a java.lang.Object)\n"
            + "        - locked <0x0000000000000022> (a java.lang.Object)\n"
            + "        at example.Holder.run(Holder.java:1)\n";

    ThreadDumpProcessor processor = ThreadDumpProcessor.builder().build();

    assertEquals(
        Map.of(
            "0x0000000000000011", "holder",
            "0x0000000000000022", "holder"),
        processor.buildLockToOwningThreadMapping(
            List.of(new ThreadDumpRegion(stackText, 0, stackText.length()))));
  }

  @Test
  void shouldReportMatchedLockOwnerThreadNameForIntrinsicLock() {
    // Given
    SpanContextualizer contextualizer = new SpanContextualizer(eventReader);
    String threadDump = readDumpFromResource("thread-dump3.txt");

    // When
    List<StackToSpanLinkage> results = collectResults(contextualizer, threadDump, false, true);

    // Then
    assertEquals(21, results.size());

    // check successfully mapped intrinsic locks
    assertThat(results)
        .anyMatch(
            stack ->
                "OkHttp TaskRunner".equals(stack.getStackTrace().getThreadName())
                    && "OkHttp TaskRunner"
                        .equals(stack.getStackTrace().getThreadLockData().getLockOwner()));

    StackTraceData blockedThread2 = findStack(results, "TEST-BLOCKED-2-BLOCKED");
    assertEquals("TEST-BLOCKED-2-HOLDER", blockedThread2.getThreadLockData().getLockOwner());

    StackTraceData blockedThread5 = findStack(results, "TEST-BLOCKED-5-BLOCKED");
    assertEquals("TEST-BLOCKED-5-HOLDER", blockedThread5.getThreadLockData().getLockOwner());

    // check example with ownable locks that cannot be mapped.
    // these locks are listed in thread dumps with prefix: "- parking to wait for"
    StackTraceData ownableLockWaiter = findStack(results, "TEST-OWNABLE-6-WAITER");
    assertEquals(
        "java.util.concurrent.locks.ReentrantLock$NonfairSync@b92f63cb8",
        ownableLockWaiter.getThreadLockData().getWaitingOn());
    assertNull(ownableLockWaiter.getThreadLockData().getLockOwner());
  }

  private static StackTraceData findStack(
      List<StackToSpanLinkage> results, String threadName) {
    return results.stream()
        .map(StackToSpanLinkage::getStackTrace)
        .filter(stack -> threadName.equals(stack.getThreadName()))
        .findFirst()
        .orElseThrow();
  }

  private IItem threadContextStartEvent(long threadId) {
    IItem event = mock(IItem.class);
    IType eventType = mock(IType.class);
    when(event.getType()).thenReturn(eventType);
    when(eventType.getIdentifier()).thenReturn(ContextAttached.EVENT_NAME);
    when(eventReader.getTraceId(event)).thenReturn(traceId);
    when(eventReader.getSpanId(event)).thenReturn(spanId);
    when(eventReader.getTraceFlags(event)).thenReturn(traceFlags);
    IMCThread thread = mock(IMCThread.class);
    when(thread.getThreadId()).thenReturn(threadId);
    when(eventReader.getThread(event)).thenReturn(thread);

    return event;
  }

  private static List<StackToSpanLinkage> collectResults(
      SpanContextualizer contextualizer,
      String threadDump,
      boolean onlyTracingSpans,
      boolean enableLocks) {
    return collectResults(contextualizer, threadDump, onlyTracingSpans, enableLocks, 1024);
  }

  private static List<StackToSpanLinkage> collectResults(
      SpanContextualizer contextualizer,
      String threadDump,
      boolean onlyTracingSpans,
      boolean enableLocks,
      int stackDepth) {
    EventReader eventReader = mock(EventReader.class);
    List<StackToSpanLinkage> results = new ArrayList<>();
    CpuEventExporter profilingEventExporter = results::add;
    ThreadDumpProcessor processor =
        ThreadDumpProcessor.builder()
            .eventReader(eventReader)
            .spanContextualizer(contextualizer)
            .cpuEventExporter(profilingEventExporter)
            .stackTraceFilter(new StackTraceFilter(eventReader, false))
            .onlyTracingSpans(onlyTracingSpans)
            .stackDepth(stackDepth)
            .locksEnabled(enableLocks)
            .build();

    IItem event = mock(IItem.class);
    IType eventType = mock(IType.class);
    when(event.getType()).thenReturn(eventType);
    when(eventType.getIdentifier()).thenReturn(ThreadDumpProcessor.EVENT_NAME);
    when(eventReader.getThreadDumpResult(event)).thenReturn(threadDump);

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
