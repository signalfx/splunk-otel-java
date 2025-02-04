package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.api.trace.SpanContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class TraceRegistry {
  private final Map<String, SpanContext> traceIds = new ConcurrentHashMap<>();

  public void register(SpanContext spanContext) {
    traceIds.put(spanContext.getTraceId(), spanContext);
  }

  public boolean isRegistered(SpanContext spanContext) {
    return traceIds.containsKey(spanContext.getTraceId());
  }

  public void unregister(SpanContext spanContext) {
    traceIds.remove(spanContext.getTraceId());
  }
}
