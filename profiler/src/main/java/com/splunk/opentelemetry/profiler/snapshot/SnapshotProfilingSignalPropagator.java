/*
 * 2024 Copyright (C) AppDynamics, Inc., and its affiliates
 * All Rights Reserved
 */

/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
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
    SpanContext spanContext = Span.fromContext(context).getSpanContext();
    if (volume == Volume.HIGHEST) {
      registry.register(spanContext);
    }
    return context;
  }
}
