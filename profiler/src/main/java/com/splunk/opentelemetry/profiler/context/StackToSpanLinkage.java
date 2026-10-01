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

package com.splunk.opentelemetry.profiler.context;

import com.splunk.opentelemetry.profiler.threaddump.StackTraceData;
import io.opentelemetry.api.trace.SpanContext;
import java.time.Instant;

/** A wrapper for a RecordedEvent that may or may not have accompanying span information. */
public class StackToSpanLinkage {
  private final Instant time;
  private final StackTraceData stackTrace;
  private final String sourceEventName;
  private final SpanLinkage spanLinkage;

  public StackToSpanLinkage(
      Instant time, StackTraceData stackTrace, String sourceEventName, SpanLinkage spanLinkage) {
    this.time = time;
    this.stackTrace = stackTrace;
    this.sourceEventName = sourceEventName;
    this.spanLinkage = spanLinkage;
  }

  public boolean hasSpanInfo() {
    return getSpanContext().isValid();
  }

  public Instant getTime() {
    return time;
  }

  public StackTraceData getStackTrace() {
    return stackTrace;
  }

  public SpanContext getSpanContext() {
    return spanLinkage.getSpanContext();
  }

  public Long getSpanStartThread() {
    return spanLinkage.getThreadId();
  }

  public String getSourceEventName() {
    return sourceEventName;
  }
}
