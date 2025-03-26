package com.splunk.opentelemetry.profiler.snapshot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class SpanTrackerProviderTest {
  private final SpanTrackerProvider provider = SpanTrackerProvider.INSTANCE;

  @AfterEach
  void tearDown() {
    provider.reset();
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
  void canResetConfiguredSpanTracker() {
    provider.configure(new InMemorySpanTracker());
    provider.reset();
    assertSame(SpanTracker.NOOP, provider.get());
  }
}
