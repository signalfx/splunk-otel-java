package com.splunk.opentelemetry.profiler.snapshot;

import java.util.Objects;
import java.util.function.Supplier;

class SpanTrackerProvider implements Supplier<SpanTracker> {
  public static final  SpanTrackerProvider INSTANCE = new SpanTrackerProvider();

  private SpanTracker tracker = SpanTracker.NOOP;

  @Override
  public SpanTracker get() {
    return tracker;
  }

  void configure(SpanTracker tracker) {
    this.tracker = Objects.requireNonNull(tracker);
  }

  private SpanTrackerProvider() {}
}
