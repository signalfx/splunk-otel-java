package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.api.trace.SpanContext;

interface ITraceRegistry {
  void register(SpanContext spanContext);

  boolean isRegistered(SpanContext spanContext);

  void unregister(SpanContext spanContext);
}
