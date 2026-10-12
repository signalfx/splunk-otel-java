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
import com.splunk.opentelemetry.profiler.events.JfrEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.internal.cache.weaklockfree.WeakConcurrentMap;
import java.util.function.Function;
import javax.annotation.Nullable;

class ProfilerContextStorage implements ContextStorage {
  private final ContextStorage delegate;
  private final Function<SpanContext, JfrEvent> newEvent;
  private final ThreadLocal<Span> activeSpan = ThreadLocal.withInitial(Span::getInvalid);
  private final WeakConcurrentMap<Thread, SpanContext> activeContext =
      new WeakConcurrentMap.WithInlinedExpunction<>();

  private volatile boolean enabled = false;
  private volatile boolean emitJfrEvents = false;
  private volatile boolean trackActiveContext = false;

  ProfilerContextStorage(ContextStorage delegate) {
    this(delegate, ProfilerContextStorage::newEvent, false);
  }

  @VisibleForTesting
  ProfilerContextStorage(ContextStorage delegate, Function<SpanContext, JfrEvent> newEvent) {
    this(delegate, newEvent, true);
  }

  private ProfilerContextStorage(
      ContextStorage delegate, Function<SpanContext, JfrEvent> newEvent, boolean enabled) {
    this.delegate = delegate;
    this.newEvent = newEvent;
    this.enabled = enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEmitJfrEvents(boolean emitJfrEvents) {
    this.emitJfrEvents = emitJfrEvents;
  }

  public void setTrackActiveContext(boolean trackActiveContext) {
    this.trackActiveContext = trackActiveContext;
    if (!trackActiveContext) {
      activeContext.clear();
    }
  }

  public WeakConcurrentMap<Thread, SpanContext> getActiveContextMap() {
    return activeContext;
  }

  @VisibleForTesting
  static JfrEvent newEvent(SpanContext spanContext) {
    if (spanContext.isValid()) {
      return new ContextAttached(
          spanContext.getTraceId(), spanContext.getSpanId(), spanContext.getTraceFlags().asByte());
    }
    return new ContextAttached(null, null, TraceFlags.getDefault().asByte());
  }

  @Override
  public Scope attach(Context toAttach) {
    Scope delegatedScope = delegate.attach(toAttach);
    if (!isEnabled()) {
      return delegatedScope;
    }
    Span span = Span.fromContext(toAttach);
    Span current = activeSpan.get();
    // Do nothing when active span didn't change. Also skip tracking when the current span isn't
    // sampled unless the current span is invalid. Invalid span usually means that span is missing
    // from context e.g. Context.root().makeCurrent() was called to clear the current span.
    if (span == current || (span != Span.getInvalid() && !span.getSpanContext().isSampled())) {
      return delegatedScope;
    }

    // mark new span as active and generate event
    activeSpan.set(span);
    activateSpan(span);

    return () -> {
      // restore previous active span
      activeSpan.set(current);
      activateSpan(current);
      delegatedScope.close();
    };
  }

  protected void activateSpan(Span span) {
    generateEvent(span);
    trackActiveContext(span);
  }

  private void trackActiveContext(Span span) {
    if (!trackActiveContext) {
      return;
    }

    SpanContext context = span.getSpanContext();
    if (context.isValid()) {
      activeContext.put(Thread.currentThread(), context);
    } else {
      activeContext.remove(Thread.currentThread());
    }
  }

  private void generateEvent(Span span) {
    if (!emitJfrEvents) {
      return;
    }

    SpanContext context = span.getSpanContext();
    JfrEvent event = newEvent.apply(context);
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
