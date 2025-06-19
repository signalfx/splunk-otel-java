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
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class TraceRegistry {
  private final Set<String> traceIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

  public final void register(SpanContext spanContext) {
    register(spanContext.getTraceId());
  }

  public void register(String traceId) {
    traceIds.add(traceId);
  }

  public final boolean isRegistered(SpanContext spanContext) {
    return isRegistered(spanContext.getTraceId());
  }

  public boolean isRegistered(String traceId) {
    return traceIds.contains(traceId);
  }

  public final void unregister(SpanContext spanContext) {
    unregister(spanContext.getTraceId());
  }

  public void unregister(String traceId) {
    traceIds.remove(traceId);
  }
}
