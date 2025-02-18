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

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

class SnapshotProfilingSignalPropagator implements TextMapPropagator {
  private static final Logger logger = Logger.getLogger(SnapshotProfilingSignalPropagator.class.getName());
  private static final String PROFILING_SIGNAL = "splunk.trace.snapshot.volume";

  private final TraceRegistry registry;

  SnapshotProfilingSignalPropagator(TraceRegistry registry) {
    this.registry = registry;
  }

  @Override
  public Collection<String> fields() {
    return Collections.emptyList();
//    return Collections.singletonList(PROFILING_SIGNAL);
  }

  @Override
  public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
//    SpanContext spanContext = Span.fromContext(context).getSpanContext();
//    if (registry.isRegistered(spanContext)) {
//      setter.set(carrier, PROFILING_SIGNAL, Volume.HIGHEST.toString());
//    }
  }

  @Override
  public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
    logger.info(context.toString());
    Baggage baggage = Baggage.fromContext(context);
    logger.info(baggage.toString());
    Volume volume = Volume.fromString(baggage.getEntryValue(PROFILING_SIGNAL));
    logger.info("Snapshot volume: " + volume);
    SpanContext spanContext = Span.fromContext(context).getSpanContext();

    if (volume == Volume.HIGHEST) {
      logger.info("Registering trace for profiling");
      registry.register(spanContext);
      return context;
    }

    if (!spanContext.isValid()) {
      logger.info("Root span detected, selecting trace for profiling");
      Baggage b = Baggage.builder().put(PROFILING_SIGNAL, Volume.HIGHEST.toString()).build();
      return context.with(b);
    }
    return context;
//    System.out.println(volume);
//    if (volume == Volume.OFF) {
//      return context;
//    }

//    SpanContext spanContext = Span.fromContext(context).getSpanContext();
//    if (spanContext.isValid()) {
//      registry.register(spanContext);
//    }
//    return context;
  }
}
