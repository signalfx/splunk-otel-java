package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.api.trace.SpanContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Test only version of {@link TraceRegistry} that keeps a record of every trace ID registered over
 * the lifetime of the instance.
 */
class RecordingTraceRegistry extends TraceRegistry {
  private final Set<String> registeredTraceIds = new HashSet<>();

  @Override
  public void register(SpanContext spanContext) {
    registeredTraceIds.add(spanContext.getTraceId());
    super.register(spanContext);
  }

  @Override
  public boolean isRegistered(SpanContext spanContext) {
    return super.isRegistered(spanContext);
  }

  @Override
  public void unregister(SpanContext spanContext) {
    super.unregister(spanContext);
  }

  Set<String> registeredTraceIds() {
    return Collections.unmodifiableSet(registeredTraceIds);
  }
}
