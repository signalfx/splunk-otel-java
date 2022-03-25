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

import java.util.ArrayList;
import java.util.List;

class StackTraceParser {
  private static final String STACK_LINE_PREFIX = "\tat ";
  private static final String THREAD_STATE_PREFIX = "java.lang.Thread.State: ";
  private static final String THREAD_NATIVE_ID_PREFIX = "nid=0x";

  public static StackTrace parse(String stackTrace) {
    // \\R - Any Unicode linebreak sequence
    String[] lines = stackTrace.split("\\R");
    if (lines.length < 2) {
      return null;
    }

    StackTraceBuilder builder = new StackTraceBuilder();

    parseHeader(builder, lines[0]);
    builder.threadStatus = parseThreadStatus(lines[1]);
    for (int i = 2; i < lines.length; i++) {
      StackTraceLine stl = parseStackTraceLine(lines[i]);
      if (stl != null) {
        builder.stackTraceLines.add(stl);
      }
    }

    return builder.build();
  }

  private static void parseHeader(StackTraceBuilder builder, String header) {
    if (header.indexOf('"') != 0) {
      return;
    }
    int nameEnd = header.lastIndexOf('"');
    if (nameEnd == 0) {
      return;
    }
    builder.threadName = header.substring(1, nameEnd);

    // some threads don't have an id
    int idEnd = nameEnd;
    int idStart = header.indexOf('#', nameEnd);
    if (idStart != -1) {
      idEnd = header.indexOf(' ', idStart);
      if (idEnd < 0) {
        return;
      }
      try {
        builder.threadId = Integer.parseInt(header.substring(idStart + 1, idEnd));
      } catch (NumberFormatException ignore) {
      }
    }

    int nidStart = header.indexOf(THREAD_NATIVE_ID_PREFIX, idEnd);
    if (nidStart != -1) {
      int nidEnd = header.indexOf(' ', nidStart);
      if (nidEnd < 0) {
        return;
      }
      try {
        builder.nativeThreadId =
            Integer.parseInt(
                header.substring(nidStart + THREAD_NATIVE_ID_PREFIX.length(), nidEnd), 16);
      } catch (NumberFormatException ignore) {
      }
    }
  }

  private static String parseThreadStatus(String status) {
    int i = status.indexOf(THREAD_STATE_PREFIX);
    if (i == -1) {
      return null;
    }

    return status.substring(i + THREAD_STATE_PREFIX.length());
  }

  private static StackTraceLine parseStackTraceLine(String line) {
    // we expect the stack trace line to look like
    // at java.lang.Thread.run(java.base@11.0.9.1/Thread.java:834)
    if (!line.startsWith(STACK_LINE_PREFIX)) {
      return null;
    }
    if (!line.endsWith(")")) {
      return null;
    }
    // remove "\tat " and trailing ")"
    line = line.substring(STACK_LINE_PREFIX.length(), line.length() - 1);
    int i = line.lastIndexOf('(');
    if (i == -1) {
      return null;
    }
    String classAndMethod = line.substring(0, i);
    String location = line.substring(i + 1);

    i = location.indexOf('/');
    if (i != -1) {
      location = location.substring(i + 1);
    }

    int lineNumber = -1;
    i = location.indexOf(':');
    if (i != -1) {
      try {
        lineNumber = Integer.parseInt(location.substring(i + 1));
      } catch (NumberFormatException ignored) {
      }
      location = location.substring(0, i);
    }

    return new StackTraceLine(classAndMethod, location, lineNumber);
  }

  private static class StackTraceBuilder {
    int threadId = -1;
    String threadName;
    int nativeThreadId = -1;
    String threadStatus;
    List<StackTraceLine> stackTraceLines = new ArrayList<>();

    StackTrace build() {
      return new StackTrace(this);
    }
  }

  static class StackTrace {
    final int threadId;
    final String threadName;
    final int nativeThreadId;
    final String threadStatus;
    final List<StackTraceLine> stackTraceLines;

    StackTrace(StackTraceBuilder builder) {
      this.threadId = builder.threadId;
      this.threadName = builder.threadName;
      this.nativeThreadId = builder.nativeThreadId;
      this.threadStatus = builder.threadStatus;
      this.stackTraceLines = builder.stackTraceLines;
    }
  }

  static class StackTraceLine {
    final String classAndMethod;
    final String location;
    final int lineNumber;

    StackTraceLine(String classAndMethod, String location, int lineNumber) {
      this.classAndMethod = classAndMethod;
      this.location = location;
      this.lineNumber = lineNumber;
    }
  }
}
