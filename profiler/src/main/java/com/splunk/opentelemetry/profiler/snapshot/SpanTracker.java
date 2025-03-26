package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.api.trace.SpanContext;
import java.util.Optional;

interface SpanTracker {
  SpanTracker NOOP = traceId -> Optional.empty();

  Optional<SpanContext> getActiveSpan(String traceId);
}
