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
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;

public class SpanLinkage {

  public static final SpanLinkage NONE = new SpanLinkage(null, null, null);

  @Nullable private final String spanId;
  @Nullable private final String traceId;
  @Nullable private final RecordedEvent recordedEvent;

  public SpanLinkage(String traceId, String spanId, RecordedEvent recordedEvent) {
    this.spanId = spanId;
    this.traceId = traceId;
    this.recordedEvent = recordedEvent;
  }

  @Nullable
  String getSpanId() {
    return spanId;
  }

  @Nullable
  String getTraceId() {
    return traceId;
  }

  RecordedThread getRecordedThread() {
    return recordedEvent == null ? null : recordedEvent.getThread();
  }

  Long getThreadId() {
    return recordedEvent == null ? Long.MIN_VALUE : getRecordedThread().getJavaThreadId();
  }

  boolean matches(String traceId, String spanId) {
    return this.traceId != null
        && this.traceId.equals(traceId)
        && this.spanId != null
        && this.spanId.equals(spanId);
  }
}
