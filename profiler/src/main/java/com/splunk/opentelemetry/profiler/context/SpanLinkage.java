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

import jdk.jfr.consumer.RecordedThread;

class SpanLinkage {

  static final SpanLinkage NONE = new SpanLinkage(null, null, null);

  private final String spanId;
  private final String traceId;
  private final RecordedThread recordedThread;

  SpanLinkage(String spanId, String traceId, RecordedThread recordedThread) {
    this.spanId = spanId;
    this.traceId = traceId;
    this.recordedThread = recordedThread;
  }

  String getSpanId() {
    return spanId;
  }

  String getTraceId() {
    return traceId;
  }

  RecordedThread getRecordedThread() {
    return recordedThread;
  }

  Long getThreadId() {
    return recordedThread.getJavaThreadId();
  }

  boolean matches(String traceId, String spanId) {
    return this.traceId != null
        && this.traceId.equals(traceId)
        && this.spanId != null
        && this.spanId.equals(spanId);
  }
}
