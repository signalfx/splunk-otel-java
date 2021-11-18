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

import io.opentelemetry.api.trace.SpanContext;
import java.time.Instant;

/** A wrapper for a RecordedEvent that may or may not have accompanying span information. */
public class StackToSpanLinkage {
  private final Instant time;
  private final String rawStack;
  private final String sourceEventName;
  private final SpanLinkage spanLinkage;

  public StackToSpanLinkage(
      Instant time, String rawStack, String sourceEventName, SpanLinkage spanLinkage) {
    this.time = time;
    this.rawStack = rawStack;
    this.sourceEventName = sourceEventName;
    this.spanLinkage = spanLinkage;
  }

  public boolean hasSpanInfo() {
    return getSpanContext().isValid();
  }

  public Instant getTime() {
    return time;
  }

  public String getRawStack() {
    return rawStack;
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

  public static StackToSpanLinkage withoutLinkage(
      Instant time, String rawStack, String sourceEventName) {
    return new StackToSpanLinkage(time, rawStack, sourceEventName, SpanLinkage.NONE);
  }
}
