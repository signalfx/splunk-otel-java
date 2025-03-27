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

import java.time.Duration;
import java.time.Instant;

class StackTraceBuilder {
  private Instant timestamp;
  private Duration duration;
  private long threadId;
  private String threadName;
  private Thread.State state;
  private Exception exception;
  private Snapshotting.SpanContextBuilder spanContextBuilder = Snapshotting.spanContext();

  public StackTraceBuilder with(Instant timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public StackTraceBuilder with(Duration duration) {
    this.duration = duration;
    return this;
  }

  public StackTraceBuilder withId(long threadId) {
    this.threadId = threadId;
    return this;
  }

  public StackTraceBuilder withName(String threadName) {
    this.threadName = threadName;
    return this;
  }

  public StackTraceBuilder with(Thread.State state) {
    this.state = state;
    return this;
  }

  public StackTraceBuilder with(Exception exception) {
    this.exception = exception;
    return this;
  }

  public StackTraceBuilder withTraceId(String traceId) {
    spanContextBuilder = spanContextBuilder.withTraceId(traceId);
    return this;
  }

  public StackTraceBuilder withSpanId(String spanId) {
    spanContextBuilder = spanContextBuilder.withSpanId(spanId);
    return this;
  }

  StackTrace build() {
    var spanContext = spanContextBuilder.build();
    return new StackTrace(
        timestamp,
        duration,
        threadId,
        threadName,
        state,
        exception.getStackTrace(),
        spanContext.getTraceId(),
        spanContext.getSpanId());
  }
}
