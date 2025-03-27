package com.splunk.opentelemetry.profiler.snapshot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpanTrackerProviderTest {
  private final SpanTrackerProvider provider = SpanTrackerProvider.INSTANCE;

  @AfterEach
  void teardown() {
    SpanTrackerProvider.INSTANCE.configure(SpanTracker.NOOP);
  }

  @Test
  void provideNoopSpanTrackerWhenNotConfigured() {
    assertSame(SpanTracker.NOOP, provider.get());
  }

  @Test
  void providedConfiguredSpanTracker() {
    var tracker = new InMemorySpanTracker();
    provider.configure(tracker);
    assertSame(tracker, provider.get());
  }

  @Test
  void doNotAllowNullSpanTrackers() {
    assertThrows(Exception.class, () -> provider.configure(null));
  }
}
