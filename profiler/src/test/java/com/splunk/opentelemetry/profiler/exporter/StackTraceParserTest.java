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

package com.splunk.opentelemetry.profiler.exporter;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.splunk.opentelemetry.profiler.ThreadDumpRegion;
import com.splunk.opentelemetry.profiler.exporter.StackTraceParser.StackTrace;
import com.splunk.opentelemetry.profiler.exporter.StackTraceParser.StackTraceLine;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class StackTraceParserTest {

  @Test
  void test() {
    String wallOfStacks = readDumpFromResource("thread-dump1.txt");
    ThreadDumpRegion.Iterator iterator = new ThreadDumpRegion.Iterator(wallOfStacks);

    boolean found = false;
    ThreadDumpRegion stack;
    while ((stack = iterator.findNextStack()) != null) {
      StackTrace stackTrace = StackTraceParser.parse(stack.getCurrentRegion(), 128);
      if (stackTrace == null) {
        continue;
      }

      assertNotNull(stackTrace.getThreadName());
      if (!stackTrace.getStackTraceLines().isEmpty()) {
        assertNotEquals(-1, stackTrace.getThreadId());
        assertNotNull(stackTrace.getThreadState());
      }
      assertNotEquals(-1, stackTrace.getOsThreadId());

      // for one stack trace verify exact values
      if (stackTrace.getThreadId() == 39) {
        assertEquals("container-0", stackTrace.getThreadName());
        assertEquals(0xaa03, stackTrace.getOsThreadId());
        assertEquals("TIMED_WAITING (sleeping)", stackTrace.getThreadState());
        assertEquals(3, stackTrace.getStackTraceLines().size());
        {
          StackTraceLine stackTraceLine = stackTrace.getStackTraceLines().get(0);
          assertEquals("java.lang.Thread", stackTraceLine.getClassName());
          assertEquals("sleep", stackTraceLine.getMethod());
          assertEquals("Native Method", stackTraceLine.getLocation());
          assertEquals(0, stackTraceLine.getLineNumber());
        }
        {
          StackTraceLine stackTraceLine = stackTrace.getStackTraceLines().get(1);
          assertEquals("org.apache.catalina.core.StandardServer", stackTraceLine.getClassName());
          assertEquals("await", stackTraceLine.getMethod());
          assertEquals("StandardServer.java", stackTraceLine.getLocation());
          assertEquals(570, stackTraceLine.getLineNumber());
        }
        {
          StackTraceLine stackTraceLine = stackTrace.getStackTraceLines().get(2);
          assertEquals(
              "org.springframework.boot.web.embedded.tomcat.TomcatWebServer$1",
              stackTraceLine.getClassName());
          assertEquals("run", stackTraceLine.getMethod());
          assertEquals("TomcatWebServer.java", stackTraceLine.getLocation());
          assertEquals(197, stackTraceLine.getLineNumber());
        }

        found = true;
      }
      if (stackTrace.getThreadId() == 40) {
        assertEquals(
            "java.lang.ref.ReferenceQueue$Lock@606704d98",
            stackTrace.getThreadLockData().getWaitingOn());
      }
    }
    assertTrue(found);
  }

  @Test
  void testLockData_waitingOnAndLocked() {
    String stackText =
        """
        "OkHttp TaskRunner" #49 [65283] daemon prio=5 os_prio=31 cpu=10.20ms elapsed=9.96s tid=0x00000008fe322300 nid=65283 in Object.wait()  [0x000000017462e000]
           java.lang.Thread.State: TIMED_WAITING (on object monitor)
          at java.lang.Object.wait0(java.base@21.0.4/Native Method)
          - waiting on <0x0000000301810958> (a okhttp3.internal.concurrent.TaskRunner)
          at java.lang.Object.wait(java.base@21.0.4/Object.java:366)
          at java.lang.Object.wait(java.base@21.0.4/Object.java:488)
          at okhttp3.internal.concurrent.TaskRunner$RealBackend.coordinatorWait(TaskRunner.kt:373)
          at okhttp3.internal.concurrent.TaskRunner.awaitTaskToRun(TaskRunner.kt:237)
          at okhttp3.internal.concurrent.TaskRunner$runnable$1.run(TaskRunner.kt:71)
          - locked <0x0000000301810958> (a okhttp3.internal.concurrent.TaskRunner)
          at java.util.concurrent.ThreadPoolExecutor.runWorker(java.base@21.0.4/ThreadPoolExecutor.java:1144)
          at java.util.concurrent.ThreadPoolExecutor$Worker.run(java.base@21.0.4/ThreadPoolExecutor.java:642)
          at java.lang.Thread.runWith(java.base@21.0.4/Thread.java:1596)
          at java.lang.Thread.run(java.base@21.0.4/Thread.java:1583)
        """;

    ThreadDumpRegion stack = new ThreadDumpRegion(stackText, 0, stackText.length());
    StackTrace stackTrace = StackTraceParser.parse(stack.getCurrentRegion(), 128);
    assertNotNull(stackTrace);
    assertThat(stackTrace.getStackTraceLines().size()).isEqualTo(10);
    assertThat(stackTrace.getThreadLockData().getWaitingOn())
        .isEqualTo("okhttp3.internal.concurrent.TaskRunner@301810958");
    assertThat(stackTrace.getThreadLockData().getLockedMonitors())
        .isEqualTo(List.of("okhttp3.internal.concurrent.TaskRunner@301810958"));
    assertTrue(stackTrace.getThreadLockData().getLockedSynchronizers().isEmpty());
  }

  @Test
  void testLockData_multipleWaitingOn() {
    String stackText =
        """
        "http-nio-9966-Poller" #54 daemon prio=5 os_prio=31 cpu=5.46ms elapsed=15.96s tid=0x0000000b2c4ba400 nid=0xf303 runnable  [0x0000000175516000]
           java.lang.Thread.State: RUNNABLE
                at sun.nio.ch.KQueue.poll(java.base@17.0.12/Native Method)
                at sun.nio.ch.KQueueSelectorImpl.doSelect(java.base@17.0.12/KQueueSelectorImpl.java:122)
                at sun.nio.ch.SelectorImpl.lockAndDoSelect(java.base@17.0.12/SelectorImpl.java:129)
                - locked <0x00000005d4a03490> (a sun.nio.ch.Util$2)
                - locked <0x00000005d4a01510> (a sun.nio.ch.KQueueSelectorImpl)
                at sun.nio.ch.SelectorImpl.select(java.base@17.0.12/SelectorImpl.java:141)
                at org.apache.tomcat.util.net.NioEndpoint$Poller.run(NioEndpoint.java:773)
                at java.lang.Thread.run(java.base@17.0.12/Thread.java:840)
        """;

    ThreadDumpRegion stack = new ThreadDumpRegion(stackText, 0, stackText.length());
    StackTrace stackTrace = StackTraceParser.parse(stack.getCurrentRegion(), 128);
    assertNotNull(stackTrace);
    assertThat(stackTrace.getStackTraceLines().size()).isEqualTo(6);
    assertNull(stackTrace.getThreadLockData().getWaitingOn());
    assertEquals(
        List.of(
            "sun.nio.ch.Util$2@5d4a03490", "sun.nio.ch.KQueueSelectorImpl@5d4a01510"),
        stackTrace.getThreadLockData().getLockedMonitors());
    assertTrue(stackTrace.getThreadLockData().getLockedSynchronizers().isEmpty());
    assertNull(stackTrace.getThreadLockData().getLockOwner());
  }

  static String readDumpFromResource(String resourcePath) {
    try (InputStream in = StackTraceParserTest.class.getResourceAsStream("/" + resourcePath)) {
      return new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
