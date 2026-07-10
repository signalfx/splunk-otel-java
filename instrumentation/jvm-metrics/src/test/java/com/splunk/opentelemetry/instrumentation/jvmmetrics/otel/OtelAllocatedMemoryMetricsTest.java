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

import static com.splunk.opentelemetry.instrumentation.jvmmetrics.AllocatedMemoryMetrics.METRIC_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.instrumentation.jvmmetrics.AllocatedMemoryMetrics;
import io.opentelemetry.api.metrics.LongCounterBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongCounter;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

class OtelAllocatedMemoryMetricsTest {
  private static final String DESCRIPTION = "Approximate sum of heap allocations.";

  @Test
  void shouldIgnoreMultipleInstallCallsUntilUninstalled() {
    try (TestFixture fixture = new TestFixture()) {
      OtelAllocatedMemoryMetrics metrics = new OtelAllocatedMemoryMetrics();

      metrics.install();
      metrics.install();
      metrics.install();

      assertThat(fixture.allocatedMemoryMetrics.constructed()).hasSize(1);
      verify(fixture.builder).buildWithCallback(any());
      verify(fixture.firstCounter, never()).close();
    }
  }

  @Test
  void shouldIgnoreMultipleUninstallCalls() {
    try (TestFixture fixture = new TestFixture()) {
      OtelAllocatedMemoryMetrics metrics = new OtelAllocatedMemoryMetrics();

      metrics.install();
      metrics.uninstall();
      metrics.uninstall();
      metrics.uninstall();

      assertThat(fixture.allocatedMemoryMetrics.constructed()).hasSize(1);
      verify(fixture.builder).buildWithCallback(any());
      verify(fixture.firstCounter).close();
    }
  }

  @Test
  void shouldInstallAgainAfterUninstall() {
    try (TestFixture fixture = new TestFixture()) {
      OtelAllocatedMemoryMetrics metrics = new OtelAllocatedMemoryMetrics();

      metrics.install();
      metrics.uninstall();
      metrics.install();
      metrics.uninstall();

      assertThat(fixture.allocatedMemoryMetrics.constructed()).hasSize(2);
      verify(fixture.builder, times(2)).buildWithCallback(any());
      verify(fixture.firstCounter).close();
      verify(fixture.secondCounter).close();
    }
  }

  private static class TestFixture implements AutoCloseable {
    private final Meter meter = mock(Meter.class);
    private final LongCounterBuilder builder = mock(LongCounterBuilder.class);
    private final ObservableLongCounter firstCounter = mock(ObservableLongCounter.class);
    private final ObservableLongCounter secondCounter = mock(ObservableLongCounter.class);
    private final MockedStatic<OtelMeterProvider> meterProvider;
    private final MockedConstruction<AllocatedMemoryMetrics> allocatedMemoryMetrics;

    private TestFixture() {
      when(meter.counterBuilder(METRIC_NAME)).thenReturn(builder);
      when(builder.setUnit("By")).thenReturn(builder);
      when(builder.setDescription(DESCRIPTION)).thenReturn(builder);
      when(builder.buildWithCallback(any())).thenReturn(firstCounter, secondCounter);

      meterProvider = mockStatic(OtelMeterProvider.class);
      allocatedMemoryMetrics =
          mockConstruction(
              AllocatedMemoryMetrics.class,
              (mock, context) -> when(mock.isUnavailable()).thenReturn(false));
      meterProvider.when(OtelMeterProvider::get).thenReturn(meter);
    }

    @Override
    public void close() {
      allocatedMemoryMetrics.close();
      meterProvider.close();
    }
  }
}
