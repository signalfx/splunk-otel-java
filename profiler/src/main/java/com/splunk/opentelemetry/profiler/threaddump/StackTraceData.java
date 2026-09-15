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

import java.util.ArrayList;
import java.util.List;

public class StackTraceData {
  private final int threadId;
  private final String threadName;
  private final int osThreadId;
  private final String threadState;
  private final ThreadLockData threadLockData;
  private final List<StackTraceLine> stackTraceLines;
  private final boolean truncated;

  private StackTraceData(Builder builder) {
    this.threadId = builder.getThreadId();
    this.threadName = builder.getThreadName();
    this.osThreadId = builder.getOsThreadId();
    this.threadState = builder.getThreadState();
    this.threadLockData = builder.getThreadLockData();
    this.stackTraceLines = builder.getStackTraceLines();
    this.truncated = builder.isTruncated();
  }

  static Builder builder() {
    return new Builder();
  }

  public int getThreadId() {
    return threadId;
  }

  public String getThreadName() {
    return threadName;
  }

  public int getOsThreadId() {
    return osThreadId;
  }

  public String getThreadState() {
    return threadState;
  }

  public ThreadLockData getThreadLockData() {
    return threadLockData;
  }

  public List<StackTraceLine> getStackTraceLines() {
    return stackTraceLines;
  }

  public boolean isTruncated() {
    return truncated;
  }

  public static class StackTraceLine {
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

    public String getClassName() {
      return className;
    }

    public String getMethod() {
      return method;
    }

    public String getLocation() {
      return location;
    }

    public int getLineNumber() {
      return lineNumber;
    }
  }

  static class Builder {
    private int threadId = 0;
    private String threadName;
    private int osThreadId = 0;
    private String threadState;
    private final ThreadLockData threadLockData = new ThreadLockData();
    private final List<StackTraceLine> stackTraceLines = new ArrayList<>();
    private boolean truncated;

    private Builder() {}

    StackTraceData build() {
      return new StackTraceData(this);
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

    ThreadLockData getThreadLockData() {
      return threadLockData;
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
}
