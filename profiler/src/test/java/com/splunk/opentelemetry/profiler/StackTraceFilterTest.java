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

import org.junit.jupiter.api.Test;

class StackTraceFilterTest {

  @Test
  void oneLiners() {
    StackTraceFilter filter = new StackTraceFilter(false);
    String stack =
        "\"GC Thread#1\" os_prio=0 cpu=1285.69ms elapsed=11333.59s tid=0x00007f43e0001000 nid=0xd0 runnable";
    assertFalse(filter.test(stack, 0, stack.length() - 1));
  }

  @Test
  void regularStack() {
    String stack =
        "\"http-nio-8080-BlockPoller\" #31 daemon prio=5 os_prio=0 cpu=1475.30ms elapsed=11327.18s tid=0x00007f4411ca8000 nid=0xe8 runnable  [0x00007f43c9691000]\n"
            + "   java.lang.Thread.State: RUNNABLE\n"
            + "\tat sun.nio.ch.EPoll.wait(java.base@11.0.12/Native Method)\n"
            + "\tat sun.nio.ch.EPollSelectorImpl.doSelect(java.base@11.0.12/EPollSelectorImpl.java:120)\n"
            + "\t[...]";
    StackTraceFilter filter = new StackTraceFilter(false);
    assertTrue(filter.test(stack, 0, stack.length() - 1));
  }

  @Test
  void batchedLogsExporter() {
    String stack =
        "\"Batched Logs Exporter\" #15 daemon prio=5 os_prio=0 cpu=267.95ms elapsed=436.16s tid=0x00007f5194044800 nid=0xd9 waiting on condition  [0x00007f51d0467000]\n"
            + "   java.lang.Thread.State: TIMED_WAITING (parking)\n"
            + "        at jdk.internal.misc.Unsafe.park(java.base@11.0.12/Native Method)\n"
            + "        - parking to wait for  <0x00000000c3a14ba8> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)\n"
            + "        at java.util.concurrent.locks.LockSupport.parkNanos(java.base@11.0.12/LockSupport.java:234)\n"
            + "        at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(java.base@11.0.12/AbstractQueuedSynchronizer.java:2123)\n"
            + "        [...]";
    StackTraceFilter filter = new StackTraceFilter(false);
    assertFalse(filter.test(stack, 0, stack.length() - 1));
  }

  @Test
  void batchSpanProcessor() {
    String stack =
        "\"BatchSpanProcessor_WorkerThread-1\" #12 daemon prio=5 os_prio=0 cpu=21.68ms elapsed=437.90s tid=0x00007f5200a8f000 nid=0xd4 waiting on condition  [0x00007f51d12ab000]\n"
            + "   java.lang.Thread.State: TIMED_WAITING (parking)\n"
            + "        at jdk.internal.misc.Unsafe.park(java.base@11.0.12/Native Method)\n"
            + "        - parking to wait for  <0x00000000c3401f88> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)\n"
            + "        [...]";
    StackTraceFilter filter = new StackTraceFilter(false);
    assertFalse(filter.test(stack, 0, stack.length() - 1));
  }

  @Test
  void jfrThreads() {
    String stack =
        "\"JFR Recorder Thread\" #16 daemon prio=5 os_prio=0 cpu=75.99ms elapsed=436.13s tid=0x00007f5194e83800 nid=0xda waiting on condition  [0x0000000000000000]\n"
            + "   java.lang.Thread.State: RUNNABLE\n";
    StackTraceFilter filter = new StackTraceFilter(false);
    assertFalse(filter.test(stack, 0, stack.length() - 1));
    stack =
        "\"JFR Periodic Tasks\" #17 daemon prio=5 os_prio=0 cpu=171.78ms elapsed=435.78s tid=0x00007f5194ecd800 nid=0xdb in Object.wait()  [0x00007f5187dfe000]\n"
            + "   java.lang.Thread.State: TIMED_WAITING (on object monitor)\n"
            + "        at java.lang.Object.wait(java.base@11.0.12/Native Method)\n"
            + "        [...]";
    assertFalse(filter.test(stack, 0, stack.length() - 1));
  }

  @Test
  void includeInternals() {
    String stack =
        "\"JFR Recording Scheduler\" #28 daemon prio=5 os_prio=31 cpu=0.11ms elapsed=320.50s tid=0x00007fb9cefc2000 nid=0x7203 in Object.wait()  [0x00007000126c5000]\n"
            + "   java.lang.Thread.State: WAITING (on object monitor)\n"
            + "        at java.lang.Object.wait(java.base@11.0.9.1/Native Method)\n"
            + "        - waiting on <0x0000000600ca4e70> (a java.util.TaskQueue)\n"
            + "        at java.lang.Object.wait(java.base@11.0.9.1/Object.java:328)\n"
            + "        at java.util.TimerThread.mainLoop(java.base@11.0.9.1/Timer.java:527)\n"
            + "        - waiting to re-lock in wait() <0x0000000600ca4e70> (a java.util.TaskQueue)\n"
            + "        at java.util.TimerThread.run(java.base@11.0.9.1/Timer.java:506)";
    StackTraceFilter filter = new StackTraceFilter(true);
    assertTrue(filter.test(stack, 0, stack.length() - 1));
    stack =
        "\"JFR Periodic Tasks\" #17 daemon prio=5 os_prio=0 cpu=171.78ms elapsed=435.78s tid=0x00007f5194ecd800 nid=0xdb in Object.wait()  [0x00007f5187dfe000]\n"
            + "   java.lang.Thread.State: TIMED_WAITING (on object monitor)\n"
            + "        at java.lang.Object.wait(java.base@11.0.12/Native Method)\n"
            + "        [...]";
    assertTrue(filter.test(stack, 0, stack.length() - 1));
  }

  @Test
  void twoLines() {
    String stack =
        "\"G1 Young RemSet Sampling\" os_prio=0 cpu=20.37ms elapsed=67.37s tid=0x00007f2b600eb800 nid=0xc runnable\n"
            + "\"VM Periodic Task Thread\" os_prio=0 cpu=29.95ms elapsed=65.02s tid=0x00007f2b60c75800 nid=0x1a waiting on condition\n";
    StackTraceFilter filter = new StackTraceFilter(false);
    assertFalse(filter.test(stack, 0, stack.length() - 1));
  }

  @Test
  void badLastIndex() {
    StackTraceFilter filter = new StackTraceFilter(false);
    assertFalse(filter.test(null, 0, -1));
  }

  @Test
  void lineDoesntStartWithDoubleQuote() {
    String stack = "JNI global refs: 50, weak refs: 0\n";
    StackTraceFilter filter = new StackTraceFilter(false);
    assertFalse(filter.test(stack, 0, stack.length() - 1));
  }

  @Test
  void extractFromMiddle() {
    String stack =
        "\"logback-7\" #75 daemon prio=5 os_prio=31 cpu=0.25ms elapsed=173.79s tid=0x00007fb98f0ad000 nid=0xd30b waiting on condiion  [0x0000700014a2e000]\n"
            + "   java.lang.Thread.State: WAITING (parking)\n"
            + "        at jdk.internal.misc.Unsafe.park(java.base@11.0.9.1/Native Method)\n"
            + "        - parking to wait for  <0x00000006127b80f8> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)\n"
            + "        at java.util.concurrent.ThreadPoolExecutor$Worker.run(java.base@11.0.9.1/ThreadPoolExecutor.java:628)\n"
            + "        at java.lang.Thread.run(java.base@11.0.9.1/Thread.java:834)\n";

    String beforeStack = "something above\n";
    String afterStack = "something below\n";
    String wallOfStacks = beforeStack + "\n" + stack + "\n" + afterStack;
    StackTraceFilter filter = new StackTraceFilter(false);
    boolean result =
        filter.test(
            wallOfStacks, beforeStack.length() + 1, beforeStack.length() + stack.length() + 2);
    assertTrue(result);
  }

  @Test
  void testOmitStacksEntirelyJvmInternal() {
    String stack =
        "\"logback-2\" #22 daemon prio=5 os_prio=31 cpu=2.42ms elapsed=563.96s tid=0x00007fd540\n"
            + "   java.lang.Thread.State: TIMED_WAITING (parking)\n"
            + "\tat jdk.internal.misc.Unsafe.park(java.base@11.0.9.1/Native Method)\n"
            + "\t- parking to wait for  <0x0000000600001920> (a java.util.concurrent.locks.Ab\n"
            + "\tat java.util.concurrent.locks.LockSupport.parkNanos(java.base@11.0.9.1/LockS\n"
            + "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awa\n"
            + "\tat java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ja\n"
            + "\tat java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take(ja\n"
            + "\tat java.util.concurrent.ThreadPoolExecutor.getTask(java.base@11.0.9.1/Thread\n"
            + "\tat java.util.concurrent.ThreadPoolExecutor.runWorker(java.base@11.0.9.1/Thre\n"
            + "\tat java.util.concurrent.ThreadPoolExecutor$Worker.run(java.base@11.0.9.1/Thr\n"
            + "\tat java.lang.Thread.run(java.base@11.0.9.1/Thread.java:834)\n";
    StackTraceFilter filter = new StackTraceFilter(false);
    boolean result = filter.test(stack, 0, stack.length() - 1);
    assertFalse(result);
  }
}
