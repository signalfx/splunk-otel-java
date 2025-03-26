package com.splunk.opentelemetry.profiler.snapshot;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Supplier;

class SpanTrackerProvider implements Supplier<SpanTracker> {
  public static final  SpanTrackerProvider INSTANCE = new SpanTrackerProvider();

  private SpanTracker tracker;

  @Override
  public SpanTracker get() {
    if (tracker == null) {
      return SpanTracker.NOOP;
    }
    return tracker;
  }

  void configure(SpanTracker tracker) {
    this.tracker = tracker;
  }

  @VisibleForTesting
  void reset() {
    tracker = null;
  }

  private SpanTrackerProvider() {}
}
