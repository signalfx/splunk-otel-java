package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.api.trace.SpanContext;

import java.util.Objects;

class ProfilingSpanContext {
  static ProfilingSpanContext from(SpanContext spanContext) {
    return new ProfilingSpanContext(spanContext.getTraceId(), spanContext.getSpanId());
  }

  private final String traceId;
  private final String spanId;

  private ProfilingSpanContext(String traceId, String spanId) {
    this.traceId = traceId;
    this.spanId = spanId;
  }

  public String getTraceId() {
    return traceId;
  }

  public String getSpanId() {
    return spanId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(traceId, spanId);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProfilingSpanContext that = (ProfilingSpanContext) o;
    return Objects.equals(traceId, that.traceId) && Objects.equals(spanId, that.spanId);
  }
}
