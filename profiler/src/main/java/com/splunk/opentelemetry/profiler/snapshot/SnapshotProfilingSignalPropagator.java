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
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.Collection;
import java.util.Collections;

class SnapshotProfilingSignalPropagator implements TextMapPropagator {
  private static final String PROFILING_SIGNAL = "splunk.trace.snapshot.volume";

  private final TraceRegistry registry;

  SnapshotProfilingSignalPropagator(TraceRegistry registry) {
    this.registry = registry;
  }

  @Override
  public Collection<String> fields() {
    return Collections.singletonList(PROFILING_SIGNAL);
  }

  @Override
  public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
    SpanContext spanContext = Span.fromContext(context).getSpanContext();
    if (registry.isRegistered(spanContext)) {
      setter.set(carrier, PROFILING_SIGNAL, Volume.HIGHEST.toString());
    }
  }

  @Override
  public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
    Volume volume = Volume.fromString(getter.get(carrier, PROFILING_SIGNAL));
    if (volume == Volume.OFF) {
      return context;
    }

    SpanContext spanContext = Span.fromContext(context).getSpanContext();
    if (spanContext.isValid()) {
      registry.register(spanContext);
    }
    return context;
  }
}
