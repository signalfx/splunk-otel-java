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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.context.StackToSpanLinkage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.Test;

class ThreadDumpProcessorTest {

  @Test
  void testProcessEvent() {
    String traceId = "abc123";
    String spanId = "xxxxxxyyyyyyyzyzzz";
    SpanContextualizer contextualizer = new SpanContextualizer();
    long idOfThreadRunningTheSpan = 3L;

    RecordedEvent event = mock(RecordedEvent.class);
    RecordedEvent threadStartingSpan = mock(RecordedEvent.class);
    RecordedThread threadRunningSpan = mock(RecordedThread.class);

    when(threadStartingSpan.getString("traceId")).thenReturn(traceId);
    when(threadStartingSpan.getString("spanId")).thenReturn(spanId);
    when(threadStartingSpan.getThread()).thenReturn(threadRunningSpan);
    when(threadRunningSpan.getJavaThreadId()).thenReturn(idOfThreadRunningTheSpan);

    contextualizer.updateContext(threadStartingSpan);
    EventType eventType = mock(EventType.class);
    when(event.getEventType()).thenReturn(eventType);
    when(eventType.getName()).thenReturn(ThreadDumpProcessor.EVENT_NAME);
    when(event.getString("result")).thenReturn(WALL_OF_STACKS);

    List<StackToSpanLinkage> results = new ArrayList<>();
    Consumer<StackToSpanLinkage> exportProcessor = results::add;
    ThreadDumpToStacks threadDumpToStacks = new ThreadDumpToStacks(new StackTraceFilter(false));
    ThreadDumpProcessor processor =
        ThreadDumpProcessor.builder()
            .spanContextualizer(contextualizer)
            .processor(exportProcessor)
            .threadDumpToStacks(threadDumpToStacks)
            .build();

    processor.accept(event);
    assertEquals(3, results.size());

    assertFalse(results.get(0).hasSpanInfo());
    assertTrue(
        results.get(0).getRawStack().contains("at java.lang.ref.Reference$ReferenceHandler"));

    assertTrue(results.get(1).hasSpanInfo());
    assertEquals(spanId, results.get(1).getSpanId());
    assertEquals(traceId, results.get(1).getTraceId());
    assertEquals(idOfThreadRunningTheSpan, results.get(1).getSpanStartThread());
    assertTrue(results.get(1).getRawStack().contains("AwesomeThinger.overHereDoingSpanThings"));

    assertFalse(results.get(2).hasSpanInfo());
    assertTrue(results.get(2).getRawStack().contains("0x0000000625152778"));
  }

  static final String WALL_OF_STACKS =
      "2021-06-07 16:02:56\n"
          + "Full thread dump OpenJDK 64-Bit Server VM (11.0.9.1+1 mixed mode):\n"
          + "\n"
          + "Threads class SMR info:\n"
          + "_java_thread_list=0x00007fb484055190, length=55, elements={\n"
          + "0x00007fb51702b000, 0x00007fb517030000, 0x00007fb507828000, 0x00007fb50701d800,\n"
          + "(truncated)\n"
          + "0x00007fb486067000, 0x00007fb517031000, 0x00007fb507547800, 0x00007fb4918f3000,\n"
          + "0x00007fb4860b5000, 0x00007fb491817800, 0x00007fb4859f4800\n"
          + "}\n"
          + "\n"
          + "\"Definitely something\" #2 daemon prio=10 os_prio=31 cpu=4.92ms elapsed=50.48s tid=0x00007fb51702b000 nid=0x3403 waiting on condition  [0x000070000c6d6000]\n"
          + "   java.lang.Thread.State: RUNNABLE\n"
          + "        at java.lang.ref.Reference.waitForReferencePendingList(java.base@11.0.9.1/Native Method)\n"
          + "        at java.lang.ref.Reference.processPendingReferences(java.base@11.0.9.1/Reference.java:241)\n"
          + "        at java.lang.ref.Reference$ReferenceHandler.run(java.base@11.0.9.1/Reference.java:213)\n"
          + "\n"
          + "\"AwesomeSpanHere\" #3 daemon prio=8 os_prio=31 cpu=0.41ms elapsed=50.48s tid=0x00007fb517030000 nid=0x3703 in Object.wait()  [0x000070000c7d9000]\n"
          + "   java.lang.Thread.State: WAITING (on object monitor)\n"
          + "        at java.lang.Object.wait(java.base@11.0.9.1/Native Method)\n"
          + "        - waiting on <0x000000060066b908> (a java.lang.ref.ReferenceQueue$Lock)\n"
          + "        at java.lang.ref.ReferenceQueue.remove(java.base@11.0.9.1/ReferenceQueue.java:155)\n"
          + "        - waiting to re-lock in wait() <0x000000060066b908> (a java.lang.ref.ReferenceQueue$Lock)\n"
          + "        at com.something.something.AwesomeThinger.overHereDoingSpanThings(MyServer.java:123)\n"
          + "\n"
          + "\"Cool user thread\" #27 daemon prio=5 os_prio=31 cpu=0.13ms elapsed=48.39s tid=0x00007fb4b74b3000 nid=0x15103 in Object.wait()  [0x000070000ed4b000]\n"
          + "   java.lang.Thread.State: WAITING (on object monitor)\n"
          + "        at java.lang.Object.wait(java.base@11.0.9.1/Native Method)\n"
          + "        - waiting on <0x0000000625152778> (a java.util.TaskQueue)\n"
          + "        at java.lang.Object.wait(java.base@11.0.9.1/Object.java:328)\n"
          + "        at java.util.TimerThread.mainLoop(java.base@11.0.9.1/Timer.java:527)\n"
          + "        - waiting to re-lock in wait() <0x0000000625152778> (a java.util.TaskQueue)\n"
          + "        at java.util.TimerThread.run(java.base@11.0.9.1/Timer.java:506)";
}
