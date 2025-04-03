package com.splunk.opentelemetry.profiler.snapshot;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.trace.SpanContext;
import java.util.Objects;
import java.util.function.Supplier;

class StackTraceSamplerProvider implements Supplier<StackTraceSampler> {
  static final StackTraceSamplerProvider INSTANCE = new StackTraceSamplerProvider();

  static final StackTraceSampler NOOP = new StackTraceSampler() {
    @Override
    public void start(SpanContext spanContext) {}
    @Override
    public void stop(SpanContext spanContext) {}
  };

  private StackTraceSampler sampler = NOOP;

  @Override
  public StackTraceSampler get() {
    return sampler;
  }

  void configure(StackTraceSampler sampler) {
    this.sampler = Objects.requireNonNull(sampler);
  }

  @VisibleForTesting
  void reset() {
    sampler = NOOP;
  }
}
