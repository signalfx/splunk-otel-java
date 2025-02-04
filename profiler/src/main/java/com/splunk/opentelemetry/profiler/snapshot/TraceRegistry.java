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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class TraceRegistry {
  private final Map<String, SpanContext> traceIds = new ConcurrentHashMap<>();

  public void register(SpanContext spanContext) {
    traceIds.put(spanContext.getTraceId(), spanContext);
  }

  public boolean isRegistered(SpanContext spanContext) {
    return traceIds.containsKey(spanContext.getTraceId());
  }

  public void unregister(SpanContext spanContext) {
    traceIds.remove(spanContext.getTraceId());
  }
}
