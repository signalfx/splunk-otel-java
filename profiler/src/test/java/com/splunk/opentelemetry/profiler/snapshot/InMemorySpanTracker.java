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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class InMemorySpanTracker implements SpanTracker {
  private final Map<String, SpanContext> stackTraces = new HashMap<>();

  void store(String traceId, SpanContext spanContext) {
    stackTraces.put(traceId, spanContext);
  }

  @Override
  public Optional<SpanContext> getActiveSpan(String traceId) {
    return Optional.ofNullable(stackTraces.get(traceId));
  }
}
