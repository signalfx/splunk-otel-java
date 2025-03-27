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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;

class ActiveSpanTracker implements ContextStorage, SpanTracker {
  private final ConcurrentMap<String, SpanContext> activeSpans = new ConcurrentHashMap<>();

  private final ContextStorage delegate;
  private final TraceRegistry registry;

  ActiveSpanTracker(ContextStorage delegate, TraceRegistry registry) {
    this.delegate = delegate;
    this.registry = registry;
  }

  @Override
  public Scope attach(Context toAttach) {
    Scope scope = delegate.attach(toAttach);
    SpanContext spanContext = Span.fromContext(toAttach).getSpanContext();
    if (doNotTrack(spanContext)) {
      return scope;
    }

    String traceId = spanContext.getTraceId();
    SpanContext current = activeSpans.get(traceId);
    if (current == spanContext) {
      return scope;
    }

    SpanContext previous = activeSpans.put(traceId, spanContext);
    return () -> {
      activeSpans.computeIfPresent(traceId, (id, sc) -> previous);
      scope.close();
    };
  }

  private boolean doNotTrack(SpanContext spanContext) {
    return !spanContext.isSampled() || !registry.isRegistered(spanContext);
  }

  @Nullable
  @Override
  public Context current() {
    return delegate.current();
  }

  @Override
  public Optional<SpanContext> getActiveSpan(String traceId) {
    return Optional.ofNullable(activeSpans.get(traceId));
  }
}
