package com.splunk.opentelemetry.profiler.snapshot;

import java.util.Optional;
import java.util.function.Supplier;

class SpanTrackerProvider implements Supplier<SpanTracker> {
  public static final  SpanTrackerProvider INSTANCE = new SpanTrackerProvider();

  private static final SpanTracker NOOP = traceId -> Optional.empty();

  private SpanTracker tracker;

  @Override
  public SpanTracker get() {
    if (tracker == null) {
      return NOOP;
    }
    return tracker;
  }

  void configure(SpanTracker tracker) {
    this.tracker = tracker;
  }

  private SpanTrackerProvider() {}
}
