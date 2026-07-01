/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.opentelemetry.instrumentation.jvmmetrics.otel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.instrumentation.jvmmetrics.GcMemoryMetrics;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongCounterBuilder;
import io.opentelemetry.api.metrics.Meter;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

class OtelGcMemoryMetricsTest {
  private static final String GC_PAUSE_COUNT_DESCRIPTION =
      "Number of gc pauses. This metric will be removed in a future release.";
  private static final String GC_PAUSE_TIME_DESCRIPTION =
      "Time spent in GC pause. This metric will be removed in a future release.";

  @Test
  void shouldIgnoreMultipleInstallCallsUntilUninstalled() {
    try (TestFixture fixture = new TestFixture()) {
      OtelGcMemoryMetrics metrics = new OtelGcMemoryMetrics();

      metrics.install();
      metrics.install();
      metrics.install();

      assertThat(fixture.gcMemoryMetrics.constructed()).hasSize(1);
      GcMemoryMetrics firstGcMemoryMetrics = fixture.gcMemoryMetrics.constructed().get(0);
      verify(firstGcMemoryMetrics).registerListener(any());
      verify(firstGcMemoryMetrics, never()).close();
      verify(fixture.gcPauseCounterBuilder).build();
      verify(fixture.gcPauseTimeBuilder).build();
    }
  }

  @Test
  void shouldIgnoreMultipleUninstallCalls() {
    try (TestFixture fixture = new TestFixture()) {
      OtelGcMemoryMetrics metrics = new OtelGcMemoryMetrics();

      metrics.install();
      metrics.uninstall();
      metrics.uninstall();
      metrics.uninstall();

      assertThat(fixture.gcMemoryMetrics.constructed()).hasSize(1);
      GcMemoryMetrics firstGcMemoryMetrics = fixture.gcMemoryMetrics.constructed().get(0);
      verify(firstGcMemoryMetrics).registerListener(any());
      verify(firstGcMemoryMetrics).close();
      verify(fixture.gcPauseCounterBuilder).build();
      verify(fixture.gcPauseTimeBuilder).build();
    }
  }

  @Test
  void shouldInstallAgainAfterUninstall() {
    try (TestFixture fixture = new TestFixture()) {
      OtelGcMemoryMetrics metrics = new OtelGcMemoryMetrics();

      metrics.install();
      metrics.uninstall();
      metrics.install();
      metrics.uninstall();

      assertThat(fixture.gcMemoryMetrics.constructed()).hasSize(2);
      for (GcMemoryMetrics constructedGcMemoryMetrics : fixture.gcMemoryMetrics.constructed()) {
        verify(constructedGcMemoryMetrics).registerListener(any());
        verify(constructedGcMemoryMetrics).close();
      }
      verify(fixture.gcPauseCounterBuilder, times(2)).build();
      verify(fixture.gcPauseTimeBuilder, times(2)).build();
    }
  }

  private static class TestFixture implements AutoCloseable {
    private final Meter meter = mock(Meter.class);
    private final LongCounterBuilder gcPauseCounterBuilder = mock(LongCounterBuilder.class);
    private final LongCounterBuilder gcPauseTimeBuilder = mock(LongCounterBuilder.class);
    private final LongCounter gcPauseCounter = mock(LongCounter.class);
    private final LongCounter gcPauseTime = mock(LongCounter.class);
    private final MockedStatic<OtelMeterProvider> meterProvider;
    private final MockedConstruction<GcMemoryMetrics> gcMemoryMetrics;

    private TestFixture() {
      when(meter.counterBuilder("jvm.gc.pause.count")).thenReturn(gcPauseCounterBuilder);
      when(gcPauseCounterBuilder.setUnit("{gcs}")).thenReturn(gcPauseCounterBuilder);
      when(gcPauseCounterBuilder.setDescription(GC_PAUSE_COUNT_DESCRIPTION))
          .thenReturn(gcPauseCounterBuilder);
      when(gcPauseCounterBuilder.build()).thenReturn(gcPauseCounter);

      when(meter.counterBuilder("jvm.gc.pause.totalTime")).thenReturn(gcPauseTimeBuilder);
      when(gcPauseTimeBuilder.setUnit("ms")).thenReturn(gcPauseTimeBuilder);
      when(gcPauseTimeBuilder.setDescription(GC_PAUSE_TIME_DESCRIPTION))
          .thenReturn(gcPauseTimeBuilder);
      when(gcPauseTimeBuilder.build()).thenReturn(gcPauseTime);

      meterProvider = mockStatic(OtelMeterProvider.class);
      gcMemoryMetrics =
          mockConstruction(
              GcMemoryMetrics.class,
              (mock, context) -> when(mock.isUnavailable()).thenReturn(false));
      meterProvider.when(OtelMeterProvider::get).thenReturn(meter);
    }

    @Override
    public void close() {
      gcMemoryMetrics.close();
      meterProvider.close();
    }
  }
}
