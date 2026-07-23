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
import java.lang.management.ThreadInfo;
import java.time.Duration;
import java.time.Instant;

class StackTrace {
  static StackTrace from(
      Instant timestamp,
      Duration duration,
      ThreadInfo thread,
      String traceId,
      String spanId,
      long recordingThreadId) {
    return new StackTrace(timestamp, duration, thread, traceId, spanId, recordingThreadId);
  }

  private final Instant timestamp;
  private final Duration duration;
  private final ThreadInfo threadInfo;
  private final String traceId;
  private final String spanId;
  private final long recordingThreadId;

  @VisibleForTesting
  StackTrace(
      Instant timestamp,
      Duration duration,
      ThreadInfo threadInfo,
      String traceId,
      String spanId,
      long recordingThreadId) {
    this.timestamp = timestamp;
    this.duration = duration;
    this.threadInfo = threadInfo;
    this.traceId = traceId;
    this.spanId = spanId;
    this.recordingThreadId = recordingThreadId;
  }

  Instant getTimestamp() {
    return timestamp;
  }

  Duration getDuration() {
    return duration;
  }

  long getThreadId() {
    return threadInfo.getThreadId();
  }

  String getThreadName() {
    return threadInfo.getThreadName();
  }

  Thread.State getThreadState() {
    return threadInfo.getThreadState();
  }

  StackTraceElement[] getStackFrames() {
    return threadInfo.getStackTrace();
  }

  ThreadInfo getThreadInfo() {
    return threadInfo;
  }

  String getTraceId() {
    return traceId;
  }

  String getSpanId() {
    return spanId;
  }

  long getRecordingThreadId() {
    return recordingThreadId;
  }
}
