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

import com.splunk.opentelemetry.profiler.ThreadDumpRegion;
import com.splunk.opentelemetry.profiler.exporter.StackTraceParser.StackTrace;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StackTraceParserTest {

  @Test
  void test() {
    String wallOfStacks = readDumpFromResource("thread-dump1.txt");
    ThreadDumpRegion stack = new ThreadDumpRegion(wallOfStacks, 0, 0);

    boolean found = false;
    while (stack.findNextStack()) {
      StackTrace stackTrace = StackTraceParser.parse(stack.getCurrentRegion());
      if (stackTrace == null) {
        continue;
      }
      System.err.println(
          stackTrace.threadName
              + " "
              + stackTrace.threadId
              + " "
              + stackTrace.nativeThreadId
              + " "
              + stackTrace.stackTraceLines.size());
      Assertions.assertNotNull(stackTrace.threadName);
      if (!stackTrace.stackTraceLines.isEmpty()) {
        Assertions.assertNotEquals(-1, stackTrace.threadId);
        Assertions.assertNotNull(stackTrace.threadStatus);
      }
      Assertions.assertNotEquals(-1, stackTrace.nativeThreadId);

      if (stackTrace.threadId == 39) {
        Assertions.assertEquals("container-0", stackTrace.threadName);
        Assertions.assertEquals(0xaa03, stackTrace.nativeThreadId);
        Assertions.assertEquals("TIMED_WAITING (sleeping)", stackTrace.threadStatus);
        Assertions.assertEquals(3, stackTrace.stackTraceLines.size());
        found = true;
      }
    }
    Assertions.assertTrue(found);
  }

  static String readDumpFromResource(String resourcePath) {
    try (InputStream in = StackTraceParserTest.class.getResourceAsStream("/" + resourcePath)) {
      return new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
