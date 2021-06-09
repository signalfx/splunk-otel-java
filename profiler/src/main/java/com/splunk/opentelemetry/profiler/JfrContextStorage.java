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
import java.util.function.BiFunction;
import javax.annotation.Nullable;

class JfrContextStorage implements ContextStorage {

  private final ContextStorage delegate;
  private final BiFunction<SpanContext, Byte, ContextAttached> newEvent;

  JfrContextStorage(ContextStorage delegate) {
    this(delegate, JfrContextStorage::newEvent);
  }

  @VisibleForTesting
  JfrContextStorage(
      ContextStorage delegate, BiFunction<SpanContext, Byte, ContextAttached> newEvent) {
    this.delegate = delegate;
    this.newEvent = newEvent;
  }

  static ContextAttached newEvent(SpanContext spanContext, byte direction) {
    return new ContextAttached(spanContext.getTraceId(), spanContext.getSpanId(), direction);
  }

  @Override
  public Scope attach(Context toAttach) {
    Scope delegatedScope = delegate.attach(toAttach);
    Span span = Span.fromContext(toAttach);
    generateEvent(span, ContextAttached.IN);
    return () -> {
      generateEvent(span, ContextAttached.OUT);
      delegatedScope.close();
    };
  }

  private void generateEvent(Span span, byte direction) {
    if (!span.getSpanContext().isValid()) {
      return;
    }
    ContextAttached event = newEvent.apply(span.getSpanContext(), direction);
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
