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

import java.util.function.Consumer;

public class StackTraceParser {
  private static final String STACK_LINE_PREFIX = "at ";
  private static final String THREAD_STATE_PREFIX = "java.lang.Thread.State: ";
  private static final String THREAD_NATIVE_ID_PREFIX = "nid=0x";

  private static final String WAITING_ON_PREFIX = "- waiting on ";
  private static final String WAITING_TO_RELOCK_PREFIX = "- waiting to re-lock in wait() ";
  private static final String LOCKED_PREFIX = "- locked ";
  private static final String PARKING_TO_WAIT_FOR_PREFIX = "- parking to wait for";
  private static final String WAITING_TO_LOCK_PREFIX = "- waiting to lock ";

  public static StackTraceData parse(String stackTrace, int stackDepth, boolean parseLockData) {
    // \\R - Any Unicode linebreak sequence
    String[] lines = stackTrace.split("\\R");
    if (lines.length < 2) {
      return null;
    }

    StackTraceData.Builder builder = StackTraceData.builder();

    parseHeader(builder, lines[0]);
    builder.setThreadState(parseThreadState(lines[1]));
    int stackTraceLineCount = 0;
    for (int i = 2; i < lines.length; i++) {
      boolean canStoreNextLine = stackTraceLineCount < stackDepth;
      // When lock reporting is disabled, there is no reason to scan the rest of the region
      // after enough stack frames have been collected. When it is enabled, continue parsing the
      // entire region so lock lines below the retained stack frames are still found.
      if (!parseLockData && !canStoreNextLine) {
        builder.setTruncated();
        break;
      }
      if (parseLine(builder, lines[i], parseLockData, canStoreNextLine)) {
        stackTraceLineCount++;
      }
    }

    return builder.build();
  }

  /** Returns {@code true} if parsed line was retained as a stacktrace element. */
  private static boolean parseLine(
      StackTraceData.Builder builder,
      String line,
      boolean parseLockData,
      boolean retainStackTraceLine) {
    int startIndex = findStartIndex(line);
    StackTraceData.StackTraceLine stackTraceLine = parseStackTraceLine(line, startIndex);
    if (stackTraceLine != null) {
      if (retainStackTraceLine) {
        builder.addStackTraceLine(stackTraceLine);
        return true;
      } else {
        builder.setTruncated();
      }

    } else if (parseLockData) {
      // If line was not recognized as code location line then it may be a lock information line
      parseLockLine(builder, line, startIndex);
    }
    return false;
  }

  private static void parseLockLine(StackTraceData.Builder builder, String line, int startIndex) {
    if (line.startsWith(WAITING_ON_PREFIX, startIndex)) {
      builder
          .getThreadLockData()
          .setWaitingOnReleasedMonitor(parseLock(line, startIndex, WAITING_ON_PREFIX));
    } else if (line.startsWith(WAITING_TO_RELOCK_PREFIX, startIndex)) {
      builder
          .getThreadLockData()
          .setWaitingOnReleasedMonitor(parseLock(line, startIndex, WAITING_TO_RELOCK_PREFIX));
    } else if (line.startsWith(WAITING_TO_LOCK_PREFIX, startIndex)) {
      builder.getThreadLockData().setWaitingOn(parseLock(line, startIndex, WAITING_TO_LOCK_PREFIX));
    } else if (line.startsWith(PARKING_TO_WAIT_FOR_PREFIX, startIndex)) {
      builder
          .getThreadLockData()
          .setWaitingOn(parseLock(line, startIndex, PARKING_TO_WAIT_FOR_PREFIX));
    } else if (line.startsWith(LOCKED_PREFIX, startIndex)) {
      String lock = parseLock(line, startIndex, LOCKED_PREFIX);
      if (lock != null) {
        builder.getThreadLockData().addLockedMonitor(lock);
      }
    }
  }

  private static String parseLock(String line, int startIndex, String prefix) {
    int objectStart = line.indexOf('<', startIndex + prefix.length());
    if (objectStart == -1) {
      return null;
    }
    int objectEnd = line.indexOf('>', objectStart + 1);
    if (objectEnd == -1) {
      return null;
    }

    String classPrefix = "(a ";
    int classStart = line.indexOf(classPrefix, objectEnd + 1);
    if (classStart == -1) {
      return null;
    }
    int classEnd = line.indexOf(')', classStart + classPrefix.length());
    if (classEnd == -1) {
      return null;
    }

    String objectId = line.substring(objectStart + 1, objectEnd);
    String className = line.substring(classStart + classPrefix.length(), classEnd);
    return formatLock(objectId, className);
  }

  static String formatLock(String objectId, String className) {
    if (objectId.startsWith("0x")) {
      objectId = objectId.substring(2);
    }
    int firstNonZero = 0;
    while (firstNonZero < objectId.length() - 1 && objectId.charAt(firstNonZero) == '0') {
      firstNonZero++;
    }

    return className + '@' + objectId.substring(firstNonZero);
  }

  private static int findStartIndex(String line) {
    int lineLength = line.length();
    int startIndex = 0;
    while (startIndex < lineLength && Character.isWhitespace(line.charAt(startIndex))) {
      startIndex++;
    }
    return startIndex;
  }

  private static void parseHeader(StackTraceData.Builder builder, String header) {
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

  private static StackTraceData.StackTraceLine parseStackTraceLine(String line, int startIndex) {
    // we expect the stack trace line to look like
    // at java.lang.Thread.run(java.base@11.0.9.1/Thread.java:834)
    if (!line.endsWith(")")) {
      return null;
    }

    // Skip white spaces and check for stack trace code location prefix
    if (!line.startsWith(STACK_LINE_PREFIX, startIndex)) {
      return null;
    }
    // remove "at " and trailing ")"
    line = line.substring(startIndex + STACK_LINE_PREFIX.length(), line.length() - 1);
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

    int lineNumber = 0;
    i = location.indexOf(':');
    if (i != -1) {
      try {
        lineNumber = Integer.parseInt(location.substring(i + 1));
      } catch (NumberFormatException ignored) {
      }
      location = location.substring(0, i);
    }

    return new StackTraceData.StackTraceLine(className, method, location, lineNumber);
  }
}
