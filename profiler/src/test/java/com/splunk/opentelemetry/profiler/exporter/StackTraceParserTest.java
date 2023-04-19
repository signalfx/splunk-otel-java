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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.splunk.opentelemetry.profiler.ThreadDumpRegion;
import com.splunk.opentelemetry.profiler.exporter.StackTraceParser.StackTrace;
import com.splunk.opentelemetry.profiler.exporter.StackTraceParser.StackTraceLine;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class StackTraceParserTest {

  @Test
  void test() {
    String wallOfStacks = readDumpFromResource("thread-dump1.txt");
    ThreadDumpRegion stack = new ThreadDumpRegion(wallOfStacks, 0, 0);

    boolean found = false;
    while (stack.findNextStack()) {
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
          assertEquals(-1, stackTraceLine.getLineNumber());
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
    }
    assertTrue(found);
  }

  static String readDumpFromResource(String resourcePath) {
    try (InputStream in = StackTraceParserTest.class.getResourceAsStream("/" + resourcePath)) {
      return new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
