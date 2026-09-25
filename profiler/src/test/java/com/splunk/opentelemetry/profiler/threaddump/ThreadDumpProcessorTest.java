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

  private static final String LOCK_OWNER_WITHOUT_SPAN_STACK =
      String.join(
          "\n",
          "\"lock-owner\" #101 prio=5",
          "   java.lang.Thread.State: WAITING",
          "        at example.LockOwner.run(LockOwner.java:1)",
          "        - locked <0x0000000000000001> (a java.lang.Object)");

  private static final String LOCK_WAITER_WITH_SPAN_STACK =
      String.join(
          "\n",
          "\"lock-waiter\" #102 prio=5",
          "   java.lang.Thread.State: BLOCKED",
          "        at example.LockWaiter.run(LockWaiter.java:1)",
          "        - waiting to lock <0x0000000000000001> (a java.lang.Object)");

  private static final String OBJECT_WAIT_STACK =
      String.join(
          "\n",
          "\"waiting-thread\" #103 prio=5",
          "   java.lang.Thread.State: TIMED_WAITING (on object monitor)",
          "        at java.lang.Object.wait0(Native Method)",
          "        - waiting on <0x0000000000000001> (a java.lang.Object)",
          "        at example.Waiter.waitForWork(Waiter.java:1)",
          "        - locked <0x0000000000000001> (a java.lang.Object)");

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

    // first stack trace
    StackToSpanLinkage stackToSpanLinkage = results.get(0);
    List<StackTraceData.StackTraceLine> stackTraceLines =
        stackToSpanLinkage.getStackTrace().getStackTraceLines();
    StackTraceData.StackTraceLine stackTraceLine = stackTraceLines.get(stackTraceLines.size() - 1);
    assertFalse(stackToSpanLinkage.hasSpanInfo());
    assertEquals("java.lang.ref.Reference$ReferenceHandler", stackTraceLine.getClassName());
    assertEquals("run", stackTraceLine.getMethod());

    // second stack trace
    stackToSpanLinkage = results.get(1);
    stackTraceLines = stackToSpanLinkage.getStackTrace().getStackTraceLines();
    stackTraceLine = stackTraceLines.get(stackTraceLines.size() - 1);
    assertTrue(stackToSpanLinkage.hasSpanInfo());
    assertEquals(expectedContext, stackToSpanLinkage.getSpanContext());
    assertEquals(idOfThreadRunningTheSpan, stackToSpanLinkage.getSpanStartThread());
    assertEquals("com.something.something.AwesomeThinger", stackTraceLine.getClassName());
    assertEquals("overHereDoingSpanThings", stackTraceLine.getMethod());

    // third stack trace
    stackToSpanLinkage = results.get(2);
    assertFalse(stackToSpanLinkage.hasSpanInfo());
    assertNull(stackToSpanLinkage.getStackTrace().getThreadLockData().getWaitingOn());
    assertTrue(
        stackToSpanLinkage.getStackTrace().getThreadLockData().getLockedMonitors().isEmpty());
  }

  @Test
  void testFilterInternalStacks() {
    SpanContextualizer contextualizer = new SpanContextualizer(new EventReader());

    String threadDump = readDumpFromResource("thread-dump1.txt");
    List<StackToSpanLinkage> results = collectResults(contextualizer, threadDump, false, false);

    assertEquals(27, results.size());

    Stream.of(StackTraceFilter.UNWANTED_PREFIXES)
        .map(
            prefix ->
                prefix.endsWith("\"")
                    ? prefix.substring(1, prefix.length() - 1)
                    : prefix.substring(1))
        .forEach(
            prefix -> {
              assertThat(results)
                  .noneMatch(stack -> stack.getStackTrace().getThreadName().startsWith(prefix));
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

    assertEquals(1, results.get(0).getStackTrace().getStackTraceLines().size());
    assertTrue(results.get(0).getStackTrace().isTruncated());
  }

  @Test
  void shouldReportMatchedLockOwnerThreadName() {
    // Given
    SpanContextualizer contextualizer = new SpanContextualizer(eventReader);
    String threadDump = readDumpFromResource("thread-dump3.txt");

    // When
    List<StackToSpanLinkage> results = collectResults(contextualizer, threadDump, false, true);

    // Then
    assertEquals(36, results.size());

    // check successfully mapped intrinsic locks
    StackTraceData intrinsicDeadlockedThread2A =
        findWaitingStack(results, "TEST-INTRINSIC-DEADLOCK-2-A");
    assertEquals(
        "TEST-INTRINSIC-DEADLOCK-2-B",
        intrinsicDeadlockedThread2A.getThreadLockData().getLockOwner());

    StackTraceData intrinsicDeadlockedThread2B =
        findWaitingStack(results, "TEST-INTRINSIC-DEADLOCK-2-B");
    assertEquals(
        "TEST-INTRINSIC-DEADLOCK-2-A",
        intrinsicDeadlockedThread2B.getThreadLockData().getLockOwner());

    StackTraceData blockedThread3 = findStack(results, "TEST-BLOCKED-3-BLOCKED");
    assertEquals("TEST-BLOCKED-3-HOLDER", blockedThread3.getThreadLockData().getLockOwner());

    // Ownable synchronizer owners are available in the deadlock summary.
    StackTraceData deadlockedThread1A = findWaitingStack(results, "TEST-DEADLOCK-1-A");
    assertEquals("TEST-DEADLOCK-1-B", deadlockedThread1A.getThreadLockData().getLockOwner());
    assertThat(deadlockedThread1A.getThreadLockData().getLockedSynchronizers())
        .containsExactly("java.util.concurrent.locks.ReentrantLock$NonfairSync@5e3ba30a0");

    StackTraceData deadlockedThread1B = findWaitingStack(results, "TEST-DEADLOCK-1-B");
    assertEquals("TEST-DEADLOCK-1-A", deadlockedThread1B.getThreadLockData().getLockOwner());
    assertThat(deadlockedThread1B.getThreadLockData().getLockedSynchronizers())
        .containsExactly("java.util.concurrent.locks.ReentrantLock$NonfairSync@5e3ba30d0");

    // Non-deadlocked ownable synchronizer owners are not present in a jdk.ThreadDump event.
    StackTraceData ownableLockWaiter = findStack(results, "TEST-OWNABLE-4-WAITER");
    assertEquals(
        "java.util.concurrent.locks.ReentrantLock$NonfairSync@5e3ff0480",
        ownableLockWaiter.getThreadLockData().getWaitingOn());
    assertNull(ownableLockWaiter.getThreadLockData().getLockOwner());
  }

  @Test
  void shouldResolveLockOwnerWithoutSpanThatAppearsBeforeWaiterWithSpan() {
    assertLockOwnerWithoutSpanIsUsed(
        String.join("\n\n", LOCK_OWNER_WITHOUT_SPAN_STACK, LOCK_WAITER_WITH_SPAN_STACK) + "\n\n");
  }

  @Test
  void shouldResolveLockOwnerWithoutSpanThatAppearsAfterWaiterWithSpan() {
    assertLockOwnerWithoutSpanIsUsed(
        String.join("\n\n", LOCK_WAITER_WITH_SPAN_STACK, LOCK_OWNER_WITHOUT_SPAN_STACK) + "\n\n");
  }

  @Test
  void shouldNotResolveWaitingThreadAsItsOwnLockOwner() {
    SpanContextualizer contextualizer = new SpanContextualizer(eventReader);

    List<StackToSpanLinkage> results =
        collectResults(contextualizer, OBJECT_WAIT_STACK + "\n\n", false, true);

    assertEquals(1, results.size());
    ThreadLockData lockData = results.get(0).getStackTrace().getThreadLockData();
    assertEquals("java.lang.Object@1", lockData.getWaitingOn());
    assertTrue(lockData.getLockedMonitors().isEmpty());
    assertNull(lockData.getLockOwner());
  }

  private void assertLockOwnerWithoutSpanIsUsed(String threadDump) {
    SpanContextualizer contextualizer = new SpanContextualizer(eventReader);
    contextualizer.updateContext(threadContextStartEvent(102));

    List<StackToSpanLinkage> results = collectResults(contextualizer, threadDump, true, true);

    assertEquals(1, results.size());
    assertTrue(results.get(0).hasSpanInfo());
    StackTraceData waiter = findStack(results, "lock-waiter");
    assertEquals("java.lang.Object@1", waiter.getThreadLockData().getWaitingOn());
    assertEquals("lock-owner", waiter.getThreadLockData().getLockOwner());
  }

  private static StackTraceData findStack(List<StackToSpanLinkage> results, String threadName) {
    return results.stream()
        .map(StackToSpanLinkage::getStackTrace)
        .filter(stack -> threadName.equals(stack.getThreadName()))
        .findFirst()
        .orElseThrow();
  }

  private static StackTraceData findWaitingStack(
      List<StackToSpanLinkage> results, String threadName) {
    return results.stream()
        .map(StackToSpanLinkage::getStackTrace)
        .filter(stack -> threadName.equals(stack.getThreadName()))
        .filter(stack -> stack.getThreadLockData().getWaitingOn() != null)
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
