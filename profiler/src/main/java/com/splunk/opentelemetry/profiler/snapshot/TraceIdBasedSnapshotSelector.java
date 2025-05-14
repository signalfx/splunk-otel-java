package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;

class TraceIdBasedSnapshotSelector implements SnapshotSelector {
  private final int percentile;

  TraceIdBasedSnapshotSelector(double selectionRate) {
    if (selectionRate < 0 || selectionRate > 1) {
      throw new IllegalArgumentException("Selection rate must be between 0 and 1.");
    }
    this.percentile = (int)(selectionRate * 100);
  }

  @Override
  public boolean select(Context context) {
    SpanContext spanContext = Span.fromContext(context).getSpanContext();
    if (!spanContext.isValid()) {
      return false;
    }

    int hash = Math.abs(spanContext.getTraceId().hashCode());
    int tracePercentile = hash % 100;
    return tracePercentile <= percentile;
  }
}
