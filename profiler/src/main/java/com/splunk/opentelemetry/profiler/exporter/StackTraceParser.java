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
import java.util.function.Consumer;

class StackTraceParser {
  private static final String STACK_LINE_PREFIX = "\tat ";
  private static final String THREAD_STATE_PREFIX = "java.lang.Thread.State: ";
  private static final String THREAD_NATIVE_ID_PREFIX = "nid=0x";

  public static StackTrace parse(String stackTrace, int stackDepth) {
    // \\R - Any Unicode linebreak sequence
    String[] lines = stackTrace.split("\\R");
    if (lines.length < 2) {
      return null;
    }

    StackTraceBuilder builder = new StackTraceBuilder();

    parseHeader(builder, lines[0]);
    builder.setThreadState(parseThreadState(lines[1]));
    for (int i = 2; i < lines.length; i++) {
      // truncate the bottom stack frames the same way as jfr stack frame limiting does
      if (i > stackDepth + 2) {
        builder.setTruncated();
        break;
      }
      StackTraceLine stackTraceLine = parseStackTraceLine(lines[i]);
      if (stackTraceLine != null) {
        builder.addStackTraceLine(stackTraceLine);
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
    builder.setThreadName(header.substring(1, nameEnd));

    int idEnd =
        parseId(
            header,
            "#",
            nameEnd,
            s -> {
              try {
                builder.setThreadId(Integer.parseInt(s));
              } catch (NumberFormatException ignore) {
              }
            });
    if (idEnd < 0) {
      return;
    }

    parseId(
        header,
        THREAD_NATIVE_ID_PREFIX,
        idEnd,
        s -> {
          try {
            builder.setOsThreadId(Integer.parseInt(s, 16));
          } catch (NumberFormatException ignore) {
          }
        });
  }

  private static int parseId(String text, String prefix, int startFrom, Consumer<String> consumer) {
    int idEnd = startFrom;
    int idStart = text.indexOf(prefix, startFrom);
    if (idStart != -1) {
      idEnd = text.indexOf(' ', idStart);
      if (idEnd != -1) {
        consumer.accept(text.substring(idStart + prefix.length(), idEnd));
      }
    }
    return idEnd;
  }

  private static String parseThreadState(String status) {
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
    int j = line.lastIndexOf('.', i);
    if (j == -1) {
      return null;
    }
    String className = line.substring(0, j);
    String method = line.substring(j + 1, i);

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

    return new StackTraceLine(className, method, location, lineNumber);
  }

  private static class StackTraceBuilder {
    private int threadId = -1;
    private String threadName;
    private int osThreadId = -1;
    private String threadState;
    private List<StackTraceLine> stackTraceLines = new ArrayList<>();
    private boolean truncated;

    StackTrace build() {
      return new StackTrace(this);
    }

    int getThreadId() {
      return threadId;
    }

    void setThreadId(int threadId) {
      this.threadId = threadId;
    }

    String getThreadName() {
      return threadName;
    }

    void setThreadName(String threadName) {
      this.threadName = threadName;
    }

    int getOsThreadId() {
      return osThreadId;
    }

    void setOsThreadId(int osThreadId) {
      this.osThreadId = osThreadId;
    }

    String getThreadState() {
      return threadState;
    }

    void setThreadState(String threadState) {
      this.threadState = threadState;
    }

    List<StackTraceLine> getStackTraceLines() {
      return stackTraceLines;
    }

    void addStackTraceLine(StackTraceLine stackTraceLine) {
      stackTraceLines.add(stackTraceLine);
    }

    boolean isTruncated() {
      return truncated;
    }

    void setTruncated() {
      this.truncated = true;
    }
  }

  static class StackTrace {
    private final int threadId;
    private final String threadName;
    private final int osThreadId;
    private final String threadState;
    private final List<StackTraceLine> stackTraceLines;
    private final boolean truncated;

    StackTrace(StackTraceBuilder builder) {
      this.threadId = builder.getThreadId();
      this.threadName = builder.getThreadName();
      this.osThreadId = builder.getOsThreadId();
      this.threadState = builder.getThreadState();
      this.stackTraceLines = builder.getStackTraceLines();
      this.truncated = builder.isTruncated();
    }

    int getThreadId() {
      return threadId;
    }

    String getThreadName() {
      return threadName;
    }

    int getOsThreadId() {
      return osThreadId;
    }

    String getThreadState() {
      return threadState;
    }

    List<StackTraceLine> getStackTraceLines() {
      return stackTraceLines;
    }

    boolean isTruncated() {
      return truncated;
    }
  }

  static class StackTraceLine {
    private final String className;
    private final String method;
    private final String location;
    private final int lineNumber;

    StackTraceLine(String className, String method, String location, int lineNumber) {
      this.className = className;
      this.method = method;
      this.location = location;
      this.lineNumber = lineNumber;
    }

    String getClassName() {
      return className;
    }

    String getMethod() {
      return method;
    }

    String getLocation() {
      return location;
    }

    int getLineNumber() {
      return lineNumber;
    }
  }
}
