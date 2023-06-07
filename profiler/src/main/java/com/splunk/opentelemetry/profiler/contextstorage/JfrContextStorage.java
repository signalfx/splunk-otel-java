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

package com.splunk.opentelemetry.profiler.contextstorage;

import com.google.common.annotations.VisibleForTesting;
import com.splunk.opentelemetry.profiler.events.ContextAttached;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.context.ContextStorage;
import java.util.function.Function;

// active context tracking for jfr profiler
public class JfrContextStorage extends AbstractContextStorage {

  private final Function<SpanContext, ContextAttached> newEvent;

  public JfrContextStorage(ContextStorage delegate) {
    this(delegate, JfrContextStorage::newEvent);
  }

  @VisibleForTesting
  JfrContextStorage(ContextStorage delegate, Function<SpanContext, ContextAttached> newEvent) {
    super(delegate);
    this.newEvent = newEvent;
  }

  static ContextAttached newEvent(SpanContext spanContext) {
    if (spanContext.isValid()) {
      return new ContextAttached(
          spanContext.getTraceId(), spanContext.getSpanId(), spanContext.getTraceFlags().asByte());
    }
    return new ContextAttached(null, null, TraceFlags.getDefault().asByte());
  }

  @Override
  protected void activateSpan(Span span) {
    SpanContext context = span.getSpanContext();
    ContextAttached event = newEvent.apply(context);
    event.begin();
    if (event.shouldCommit()) {
      event.commit();
    }
  }
}
