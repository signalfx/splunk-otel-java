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
    builder.setThreadState(parseThreadState(lines[1]));
    for (int i = 2; i < lines.length; i++) {
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

    // some threads don't have an id
    int idEnd = nameEnd;
    int idStart = header.indexOf('#', nameEnd);
    if (idStart != -1) {
      idEnd = header.indexOf(' ', idStart);
      if (idEnd < 0) {
        return;
      }
      try {
        builder.setThreadId(Integer.parseInt(header.substring(idStart + 1, idEnd)));
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
        builder.setNativeThreadId(
            Integer.parseInt(
                header.substring(nidStart + THREAD_NATIVE_ID_PREFIX.length(), nidEnd), 16));
      } catch (NumberFormatException ignore) {
      }
    }
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
    private int threadId = -1;
    private String threadName;
    private int nativeThreadId = -1;
    private String threadState;
    private List<StackTraceLine> stackTraceLines = new ArrayList<>();

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

    int getNativeThreadId() {
      return nativeThreadId;
    }

    void setNativeThreadId(int nativeThreadId) {
      this.nativeThreadId = nativeThreadId;
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
  }

  static class StackTrace {
    private final int threadId;
    private final String threadName;
    private final int nativeThreadId;
    private final String threadState;
    private final List<StackTraceLine> stackTraceLines;

    StackTrace(StackTraceBuilder builder) {
      this.threadId = builder.getThreadId();
      this.threadName = builder.getThreadName();
      this.nativeThreadId = builder.getNativeThreadId();
      this.threadState = builder.getThreadState();
      this.stackTraceLines = builder.getStackTraceLines();
    }

    int getThreadId() {
      return threadId;
    }

    String getThreadName() {
      return threadName;
    }

    int getNativeThreadId() {
      return nativeThreadId;
    }

    String getThreadState() {
      return threadState;
    }

    List<StackTraceLine> getStackTraceLines() {
      return stackTraceLines;
    }
  }

  static class StackTraceLine {
    private final String classAndMethod;
    private final String location;
    private final int lineNumber;

    StackTraceLine(String classAndMethod, String location, int lineNumber) {
      this.classAndMethod = classAndMethod;
      this.location = location;
      this.lineNumber = lineNumber;
    }

    String getClassAndMethod() {
      return classAndMethod;
    }

    String getLocation() {
      return location;
    }

    int getLineNumber() {
      return lineNumber;
    }
  }
}
