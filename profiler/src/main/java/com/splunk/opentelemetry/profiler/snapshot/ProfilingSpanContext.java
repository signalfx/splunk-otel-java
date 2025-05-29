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

import io.opentelemetry.api.trace.SpanContext;
import java.util.Objects;

class ProfilingSpanContext {
  static final ProfilingSpanContext INVALID = from(SpanContext.getInvalid());

  static ProfilingSpanContext from(SpanContext spanContext) {
    return new ProfilingSpanContext(spanContext.getTraceId(), spanContext.getSpanId());
  }

  private final String traceId;
  private final String spanId;

  private ProfilingSpanContext(String traceId, String spanId) {
    this.traceId = traceId;
    this.spanId = spanId;
  }

  public String getTraceId() {
    return traceId;
  }

  public String getSpanId() {
    return spanId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(traceId, spanId);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProfilingSpanContext that = (ProfilingSpanContext) o;
    return Objects.equals(traceId, that.traceId) && Objects.equals(spanId, that.spanId);
  }
}
