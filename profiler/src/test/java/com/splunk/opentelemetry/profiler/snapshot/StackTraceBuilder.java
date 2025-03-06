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

import java.time.Instant;

class StackTraceBuilder {
  private Instant timestamp;
  private long threadId;
  private String threadName;
  private Exception exception;

  public StackTraceBuilder with(Instant timestamp) {
    this.timestamp = timestamp;
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

  public StackTraceBuilder with(Exception exception) {
    this.exception = exception;
    return this;
  }

  StackTrace build() {
    return new StackTrace(timestamp, threadId, threadName, exception.getStackTrace());
  }
}
