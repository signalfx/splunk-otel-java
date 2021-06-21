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

import javax.annotation.Nullable;

public class SpanLinkage {

  public static final SpanLinkage NONE = new SpanLinkage(null, null, null);

  @Nullable private final String spanId;
  @Nullable private final String traceId;
  @Nullable private final Long threadId;

  public SpanLinkage(@Nullable String traceId, @Nullable String spanId, @Nullable Long threadId) {
    this.spanId = spanId;
    this.traceId = traceId;
    this.threadId = threadId;
  }

  @Nullable
  String getSpanId() {
    return spanId;
  }

  @Nullable
  String getTraceId() {
    return traceId;
  }

  @Nullable
  Long getThreadId() {
    return threadId;
  }

  boolean matches(String traceId, String spanId) {
    return this.traceId != null
        && this.traceId.equals(traceId)
        && this.spanId != null
        && this.spanId.equals(spanId);
  }
}
