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

class AgentInternalsFilterTest {

  @Test
  void testOneLiners() {
    AgentInternalsFilter filter = new AgentInternalsFilter(false);
    String stack =
        "\"GC Thread#1\" os_prio=0 cpu=1285.69ms elapsed=11333.59s tid=0x00007f43e0001000 nid=0xd0 runnable";
    assertFalse(filter.test(stack, 0, stack.length()));
  }

  @Test
  void testRegularStack() {
    String stack =
        "\"http-nio-8080-BlockPoller\" #31 daemon prio=5 os_prio=0 cpu=1475.30ms elapsed=11327.18s tid=0x00007f4411ca8000 nid=0xe8 runnable  [0x00007f43c9691000]\n"
            + "   java.lang.Thread.State: RUNNABLE\n"
            + "\tat sun.nio.ch.EPoll.wait(java.base@11.0.12/Native Method)\n"
            + "\tat sun.nio.ch.EPollSelectorImpl.doSelect(java.base@11.0.12/EPollSelectorImpl.java:120)\n"
            + "\t[...]";
    AgentInternalsFilter filter = new AgentInternalsFilter(false);
    assertTrue(filter.test(stack, 0, stack.length()));
  }

  @Test
  void testBatchedLogsExporter() {
    String stack =
        "\"Batched Logs Exporter\" #15 daemon prio=5 os_prio=0 cpu=267.95ms elapsed=436.16s tid=0x00007f5194044800 nid=0xd9 waiting on condition  [0x00007f51d0467000]\n"
            + "   java.lang.Thread.State: TIMED_WAITING (parking)\n"
            + "        at jdk.internal.misc.Unsafe.park(java.base@11.0.12/Native Method)\n"
            + "        - parking to wait for  <0x00000000c3a14ba8> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)\n"
            + "        at java.util.concurrent.locks.LockSupport.parkNanos(java.base@11.0.12/LockSupport.java:234)\n"
            + "        at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(java.base@11.0.12/AbstractQueuedSynchronizer.java:2123)\n"
            + "        [...]";
    AgentInternalsFilter filter = new AgentInternalsFilter(false);
    assertFalse(filter.test(stack, 0, stack.length()));
  }

  @Test
  void testBatchSpanProcessor() {
    String stack =
        "\"BatchSpanProcessor_WorkerThread-1\" #12 daemon prio=5 os_prio=0 cpu=21.68ms elapsed=437.90s tid=0x00007f5200a8f000 nid=0xd4 waiting on condition  [0x00007f51d12ab000]\n"
            + "   java.lang.Thread.State: TIMED_WAITING (parking)\n"
            + "        at jdk.internal.misc.Unsafe.park(java.base@11.0.12/Native Method)\n"
            + "        - parking to wait for  <0x00000000c3401f88> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)\n"
            + "        [...]";
    AgentInternalsFilter filter = new AgentInternalsFilter(false);
    assertFalse(filter.test(stack, 0, stack.length()));
  }

  @Test
  void testJFRThreads() {
    String stack =
        "\"JFR Recorder Thread\" #16 daemon prio=5 os_prio=0 cpu=75.99ms elapsed=436.13s tid=0x00007f5194e83800 nid=0xda waiting on condition  [0x0000000000000000]\n"
            + "   java.lang.Thread.State: RUNNABLE\n";
    AgentInternalsFilter filter = new AgentInternalsFilter(false);
    assertFalse(filter.test(stack, 0, stack.length()));
    stack =
        "\"JFR Periodic Tasks\" #17 daemon prio=5 os_prio=0 cpu=171.78ms elapsed=435.78s tid=0x00007f5194ecd800 nid=0xdb in Object.wait()  [0x00007f5187dfe000]\n"
            + "   java.lang.Thread.State: TIMED_WAITING (on object monitor)\n"
            + "        at java.lang.Object.wait(java.base@11.0.12/Native Method)\n"
            + "        [...]";
    assertFalse(filter.test(stack, 0, stack.length()));
  }
}
