package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;

class TraceIdBasedSnapshotSelector implements SnapshotSelector {
  private final double selectionRate;

  TraceIdBasedSnapshotSelector(double selectionRate) {
    this.selectionRate = selectionRate;
  }

  @Override
  public boolean select(Context context) {
    SpanContext spanContext = Span.fromContext(context).getSpanContext();
    if (!spanContext.isValid()) {
      return false;
    }

    int hash = Math.abs(spanContext.getTraceId().hashCode());
    int percentile = hash % 100;
    return percentile <= (selectionRate * 100);
  }
}
