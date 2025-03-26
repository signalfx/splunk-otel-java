package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.api.trace.SpanContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class InMemorySpanTracker implements SpanTracker {
  private final Map<String, SpanContext> stackTraces = new HashMap<>();

  void store(String traceId, SpanContext spanContext) {
    stackTraces.put(traceId, spanContext);
  }

  @Override
  public Optional<SpanContext> getActiveSpan(String traceId) {
    return Optional.ofNullable(stackTraces.get(traceId));
  }
}
