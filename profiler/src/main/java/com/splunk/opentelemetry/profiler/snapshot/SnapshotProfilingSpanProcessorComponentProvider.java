package com.splunk.opentelemetry.profiler.snapshot;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;

@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class SnapshotProfilingSpanProcessorComponentProvider implements ComponentProvider<SpanProcessor> {
  public static final String NAME = "splunk-snapshot-profiling";
  private static TraceRegistry traceRegistry;

  public SnapshotProfilingSpanProcessorComponentProvider() {
  }

  @Override
  public Class<SpanProcessor> getType() {
    return SpanProcessor.class;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public SnapshotProfilingSpanProcessor create(
      DeclarativeConfigProperties declarativeConfigProperties) {
    return new SnapshotProfilingSpanProcessor(traceRegistry);
  }

  static void setTraceRegistry(TraceRegistry registry) {
    traceRegistry = registry;
  }
}
