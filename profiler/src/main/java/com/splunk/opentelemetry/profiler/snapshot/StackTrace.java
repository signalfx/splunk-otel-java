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

package com.splunk.opentelemetry.profiler.snapshot;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.trace.SpanContext;
import java.lang.management.ThreadInfo;
import java.time.Duration;
import java.time.Instant;

class StackTrace {
  static StackTrace from(Instant timestamp, Duration duration, ThreadInfo thread, SpanContext spanContext) {
    return new StackTrace(
        timestamp,
        duration,
        thread.getThreadId(),
        thread.getThreadName(),
        thread.getThreadState(),
        thread.getStackTrace(),
        spanContext
        );
  }

  private final Instant timestamp;
  private final Duration duration;
  private final long threadId;
  private final String threadName;
  private final Thread.State threadState;
  private final StackTraceElement[] stackFrames;
  private final SpanContext spanContext;

  @VisibleForTesting
  StackTrace(
      Instant timestamp,
      Duration duration,
      long threadId,
      String threadName,
      Thread.State threadState,
      StackTraceElement[] stackFrames,
      SpanContext spanContext) {
    this.timestamp = timestamp;
    this.duration = duration;
    this.threadId = threadId;
    this.threadName = threadName;
    this.threadState = threadState;
    this.stackFrames = stackFrames;
    this.spanContext = spanContext;
  }

  Instant getTimestamp() {
    return timestamp;
  }

  Duration getDuration() {
    return duration;
  }

  long getThreadId() {
    return threadId;
  }

  String getThreadName() {
    return threadName;
  }

  Thread.State getThreadState() {
    return threadState;
  }

  StackTraceElement[] getStackFrames() {
    return stackFrames;
  }

  String getTraceId() {
    return spanContext.getTraceId();
  }

  String getSpanId() {
    return spanContext.getSpanId();
  }
}
