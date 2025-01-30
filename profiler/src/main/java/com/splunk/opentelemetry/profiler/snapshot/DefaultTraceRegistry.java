package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.api.trace.SpanContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class DefaultTraceRegistry implements TraceRegistry {
  private final Map<String, SpanContext> traceIds = new ConcurrentHashMap<>();

  @Override
  public void register(SpanContext spanContext) {
    traceIds.put(spanContext.getTraceId(), spanContext);
  }

  @Override
  public boolean isRegistered(SpanContext spanContext) {
    return traceIds.containsKey(spanContext.getTraceId());
  }

  @Override
  public void unregister(SpanContext spanContext) {
    traceIds.remove(spanContext.getTraceId());
  }
}
