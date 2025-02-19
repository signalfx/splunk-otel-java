package com.splunk.opentelemetry.profiler.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class SnapshotVolumePropagatorProviderTest {
  private final SnapshotVolumePropagatorProvider provider = new SnapshotVolumePropagatorProvider();

  @Test
  void provideSnapshotVolumePropagator() {
    var propagator = provider.getPropagator(DefaultConfigProperties.create(Collections.emptyMap()));
    assertInstanceOf(SnapshotVolumePropagator.class, propagator);
  }

  @Test
  void name() {
    assertEquals("splunk-snapshot", provider.getName());
  }
}
