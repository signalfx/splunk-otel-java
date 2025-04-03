package com.splunk.opentelemetry.profiler.snapshot;

import java.util.Objects;
import java.util.function.Supplier;

class StagingAreaProvider implements Supplier<StagingArea> {
  static final StagingAreaProvider INSTANCE = new StagingAreaProvider();
  static final StagingArea NOOP = new StagingArea() {
    @Override
    public void stage(String traceId, StackTrace stackTrace) {}
    @Override
    public void empty(String traceId) {}
  };

  private StagingArea stagingArea = StagingAreaProvider.NOOP;

  @Override
  public StagingArea get() {
    return stagingArea;
  }

  void configure(StagingArea stagingArea) {
    this.stagingArea = Objects.requireNonNull(stagingArea);
  }

  void reset() {
    stagingArea = StagingAreaProvider.NOOP;
  }
}
