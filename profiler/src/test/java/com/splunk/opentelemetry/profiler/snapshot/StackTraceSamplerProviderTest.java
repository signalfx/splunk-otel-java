package com.splunk.opentelemetry.profiler.snapshot;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StackTraceSamplerProviderTest {
  private final StackTraceSamplerProvider provider = StackTraceSamplerProvider.INSTANCE;

  @AfterEach
  void tearDown() {
    provider.reset();
  }

  @Test
  void provideNoopSamplerWhenNotConfigured() {
    assertSame(StackTraceSamplerProvider.NOOP, provider.get());
  }

  @Test
  void providedConfiguredSampler() {
    var sampler = new ObservableStackTraceSampler();
    provider.configure(sampler);
    assertSame(sampler, provider.get());
  }

  @Test
  void canResetProvider() {
    provider.configure(new ObservableStackTraceSampler());
    provider.reset();
    assertSame(StackTraceSamplerProvider.NOOP, provider.get());
  }
}
