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
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.Collection;
import java.util.Collections;

class SnapshotVolumePropagator implements TextMapPropagator {
  private final TraceSelector selector;

  SnapshotVolumePropagator() {
    this(new ProbabilisticTraceSelector(.01));
  }

  SnapshotVolumePropagator(TraceSelector selector) {
    this.selector = selector;
  }

  @Override
  public Collection<String> fields() {
    return Collections.emptyList();
  }

  @Override
  public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {}

  /**
   * We're attempting to infer the start of a trace by inspecting the provided {@link Context}, and
   * if so make a decision whether to select the trace for snapshotting. We will not have a trace ID
   * at this time so we'll communicate the snapshotting decision to later instrumentation steps by
   * placing an entry in OpenTelemetry's {@link io.opentelemetry.api.baggage.Baggage}. <br>
   * <br>
   * If we're somewhere downstream of the trace root we leave the {@link Context} unchanged.
   */
  @Override
  public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
    if (isTraceRoot(context) && selector.select()) {
      return context.with(Volume.HIGHEST);
    }
    return context;
  }

  private boolean isTraceRoot(Context context) {
    return Span.fromContextOrNull(context) == null;
  }
}
