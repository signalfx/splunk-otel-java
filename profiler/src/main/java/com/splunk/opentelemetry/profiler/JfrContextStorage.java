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

package com.splunk.opentelemetry.profiler;

import com.google.common.annotations.VisibleForTesting;
import com.splunk.opentelemetry.profiler.events.ContextAttached;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import java.util.function.Function;
import javax.annotation.Nullable;

class JfrContextStorage implements ContextStorage {

  private final ContextStorage delegate;
  private final Function<SpanContext, ContextAttached> newEvent;
  private final ThreadLocal<Span> activeSpan = ThreadLocal.withInitial(Span::getInvalid);

  JfrContextStorage(ContextStorage delegate) {
    this(delegate, JfrContextStorage::newEvent);
  }

  @VisibleForTesting
  JfrContextStorage(ContextStorage delegate, Function<SpanContext, ContextAttached> newEvent) {
    this.delegate = delegate;
    this.newEvent = newEvent;
  }

  static ContextAttached newEvent(SpanContext spanContext) {
    if (spanContext.isValid()) {
      return new ContextAttached(spanContext.getTraceId(), spanContext.getSpanId());
    }
    return new ContextAttached(null, null);
  }

  @Override
  public Scope attach(Context toAttach) {
    Scope delegatedScope = delegate.attach(toAttach);
    Span span = Span.fromContext(toAttach);
    Span current = activeSpan.get();
    // do nothing when active span didn't change
    // do nothing if the span isn't sampled
    if (span == current || !span.getSpanContext().isSampled()) {
      return delegatedScope;
    }

    // mark new span as active and generate event
    activeSpan.set(span);
    generateEvent(span);
    return () -> {
      // restore previous active span
      activeSpan.set(current);
      generateEvent(current);
      delegatedScope.close();
    };
  }

  private void generateEvent(Span span) {
    SpanContext context = span.getSpanContext();
    ContextAttached event = newEvent.apply(context);
    event.begin();
    if (event.shouldCommit()) {
      event.commit();
    }
  }

  @Nullable
  @Override
  public Context current() {
    return delegate.current();
  }
}
