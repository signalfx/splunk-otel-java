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
import io.opentelemetry.instrumentation.api.internal.cache.Cache;
import java.util.Optional;
import javax.annotation.Nullable;

class ActiveSpanTracker implements ContextStorage, SpanTracker {
  private final Cache<Thread, SpanContext> cache = Cache.weak();

  private final ContextStorage delegate;
  private final TraceRegistry registry;

  ActiveSpanTracker(ContextStorage delegate, TraceRegistry registry) {
    this.delegate = delegate;
    this.registry = registry;
  }

  @Override
  public Scope attach(Context toAttach) {
    Scope scope = delegate.attach(toAttach);
    SpanContext newSpanContext = Span.fromContext(toAttach).getSpanContext();
    if (doNotTrack(newSpanContext)) {
      return scope;
    }

    Thread thread = Thread.currentThread();
    SpanContext oldSpanContext = cache.get(thread);
    if (oldSpanContext == newSpanContext) {
      return scope;
    }

    cache.put(thread, newSpanContext);
    return () -> {
      if (oldSpanContext != null) {
        cache.put(thread, oldSpanContext);
      } else {
        cache.remove(thread);
      }
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

  public Optional<SpanContext> getActiveSpan(Thread thread) {
    return Optional.ofNullable(cache.get(thread));
  }
}
