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

package com.splunk.opentelemetry.profiler.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.splunk.opentelemetry.profiler.OtelLoggerFactory;
import com.splunk.opentelemetry.profiler.util.OptionalConfigurableSupplier;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SnapshotProfilingSupervisorTest {
  private static final Resource RESOURCE = Resource.empty();

  @Mock AutoConfiguredOpenTelemetrySdk sdk;
  @Mock OtelLoggerFactory otelLoggerFactory;
  @Mock SpanTracker spanTracker;
  @Mock TraceThreadChangeDetector traceThreadChangeDetector;
  @Mock SnapshotProfilingSpanProcessor profilingSpanProcessor;
  @Mock StackTraceSampler stackTraceSampler;
  @Mock StagingArea stagingArea;
  @Mock StackTraceExporter stackTraceExporter;
  @Mock ConfigProperties configProperties;

  private OptionalConfigurableSupplier<SnapshotProfilingConfiguration> configurationSupplier;
  private ConfigurableSupplier<StackTraceSampler> stackTraceSamplerSupplier;
  private ConfigurableSupplier<StagingArea> stagingAreaSupplier;
  private ConfigurableSupplier<StackTraceExporter> stackTraceExporterSupplier;
  private SnapshotProfilingSupervisor supervisor;
  private MockedStatic<AutoConfigureUtil> autoConfigureUtil;

  @BeforeEach
  void setUp() {
    configurationSupplier = new OptionalConfigurableSupplier<>();

    ConfigurableSupplier<SpanTracker> spanTrackerSupplier = new ConfigurableSupplier<>(spanTracker);
    OptionalConfigurableSupplier<TraceThreadChangeDetector> traceThreadChangeDetectorSupplier =
        new OptionalConfigurableSupplier<>();
    traceThreadChangeDetectorSupplier.configure(traceThreadChangeDetector);
    OptionalConfigurableSupplier<SnapshotProfilingSpanProcessor> profilingSpanProcessorSupplier =
        new OptionalConfigurableSupplier<>();
    profilingSpanProcessorSupplier.configure(profilingSpanProcessor);
    stackTraceSamplerSupplier = new ConfigurableSupplier<>(StackTraceSampler.NOOP);
    stagingAreaSupplier = new ConfigurableSupplier<>(StagingArea.NOOP);
    stackTraceExporterSupplier = new ConfigurableSupplier<>(StackTraceExporter.NOOP);

    supervisor =
        new SnapshotProfilingSupervisor(
            configurationSupplier,
            stagingAreaSupplier,
            stackTraceSamplerSupplier,
            stackTraceExporterSupplier,
            spanTrackerSupplier,
            traceThreadChangeDetectorSupplier,
            profilingSpanProcessorSupplier,
            sdk,
            otelLoggerFactory);

    autoConfigureUtil = mockStatic(AutoConfigureUtil.class);
    autoConfigureUtil.when(() -> AutoConfigureUtil.getResource(sdk)).thenReturn(RESOURCE);
  }

  @AfterEach
  void tearDown() {
    supervisor.stopProfiling();
    autoConfigureUtil.close();
    Snapshotting.resetProfiling();
  }

  @Test
  void initializeRegistersSupervisor() {
    SnapshotProfilingSupervisor initialized = SnapshotProfilingSupervisor.initialize(sdk);

    assertThat(SnapshotProfilingSupervisor.SUPPLIER.get()).isSameAs(initialized);
    assertThatThrownBy(() -> SnapshotProfilingSupervisor.initialize(sdk))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Snapshot profiling already initialized");
  }

  @Test
  void startProfilingOnlyOnce() {
    SnapshotProfilingConfiguration configuration = configuration(true);
    configurationSupplier.configure(configuration);

    supervisor.startProfiling();
    StackTraceSampler configuredSampler = stackTraceSamplerSupplier.get();
    StagingArea configuredStagingArea = stagingAreaSupplier.get();
    StackTraceExporter configuredExporter = stackTraceExporterSupplier.get();
    supervisor.startProfiling();

    verifyEnabled(true);
    assertThat(stackTraceSamplerSupplier.get()).isSameAs(configuredSampler);
    assertThat(stagingAreaSupplier.get()).isSameAs(configuredStagingArea);
    assertThat(stackTraceExporterSupplier.get()).isSameAs(configuredExporter);
    assertRuntimeComponentsConfigured();
  }

  @Test
  void stopProfilingOnlyOnce() {
    SnapshotProfilingConfiguration configuration = configuration(true);
    configurationSupplier.configure(configuration);
    supervisor.startProfiling();
    configureRuntimeComponents();

    supervisor.stopProfiling();
    supervisor.stopProfiling();

    verifyClosedRuntimeComponents();
    verify(spanTracker).setEnabled(true);
    verify(spanTracker).setEnabled(false);
    verify(traceThreadChangeDetector).setEnabled(true);
    verify(traceThreadChangeDetector).setEnabled(false);
    verify(profilingSpanProcessor).setEnabled(true);
    verify(profilingSpanProcessor).setEnabled(false);
    verifyNoMoreInteractions(spanTracker, traceThreadChangeDetector, profilingSpanProcessor);
    assertRuntimeComponentsReset();
  }

  @Test
  void doNotStartProfilingWhenReinitializedWithDisabledConfiguration() {
    configurationSupplier.configure(configuration(false));

    supervisor.reinitializeProfiling();

    verifyNoInteractions(spanTracker, traceThreadChangeDetector, profilingSpanProcessor);
    assertRuntimeComponentsReset();
  }

  @Test
  void restartProfilingWhenReinitializedWithEnabledConfiguration() {
    SnapshotProfilingConfiguration initialConfiguration = configuration(true);
    configurationSupplier.configure(initialConfiguration);
    supervisor.startProfiling();
    StackTraceSampler initialSampler = stackTraceSamplerSupplier.get();
    StagingArea initialStagingArea = stagingAreaSupplier.get();
    StackTraceExporter initialExporter = stackTraceExporterSupplier.get();
    configureRuntimeComponents();
    clearInvocations(spanTracker, traceThreadChangeDetector, profilingSpanProcessor);

    SnapshotProfilingConfiguration updatedConfiguration =
        configuration(true).toBuilder().setStackDepth(512).build();
    configurationSupplier.configure(updatedConfiguration);
    supervisor.reinitializeProfiling();

    verifyClosedRuntimeComponents();
    InOrder inOrder = inOrder(spanTracker, traceThreadChangeDetector, profilingSpanProcessor);
    inOrder.verify(spanTracker).setEnabled(false);
    inOrder.verify(traceThreadChangeDetector).setEnabled(false);
    inOrder.verify(profilingSpanProcessor).setEnabled(false);
    inOrder.verify(spanTracker).setEnabled(true);
    inOrder.verify(traceThreadChangeDetector).setEnabled(true);
    inOrder.verify(profilingSpanProcessor).setEnabled(true);
    inOrder.verifyNoMoreInteractions();
    assertThat(stackTraceSamplerSupplier.get()).isNotSameAs(initialSampler);
    assertThat(stagingAreaSupplier.get()).isNotSameAs(initialStagingArea);
    assertThat(stackTraceExporterSupplier.get()).isNotSameAs(initialExporter);
    assertRuntimeComponentsConfigured();
  }

  @Test
  void stopProfilingWhenReinitializedWithDisabledConfiguration() {
    SnapshotProfilingConfiguration initialConfiguration = configuration(true);
    configurationSupplier.configure(initialConfiguration);
    supervisor.startProfiling();
    configureRuntimeComponents();
    clearInvocations(spanTracker, traceThreadChangeDetector, profilingSpanProcessor);

    SnapshotProfilingConfiguration disabledConfiguration = configuration(false);
    configurationSupplier.configure(disabledConfiguration);
    supervisor.reinitializeProfiling();

    verifyClosedRuntimeComponents();
    verifyEnabled(false);
    assertRuntimeComponentsReset();
  }

  private void configureRuntimeComponents() {
    stackTraceSamplerSupplier.get().close();
    stagingAreaSupplier.get().close();
    stackTraceExporterSupplier.get().close();
    stackTraceSamplerSupplier.configure(stackTraceSampler);
    stagingAreaSupplier.configure(stagingArea);
    stackTraceExporterSupplier.configure(stackTraceExporter);
  }

  private void verifyClosedRuntimeComponents() {
    verify(stackTraceSampler).close();
    verify(stagingArea).close();
    verify(stackTraceExporter).close();
  }

  private void verifyEnabled(boolean enabled) {
    verify(spanTracker).setEnabled(enabled);
    verify(traceThreadChangeDetector).setEnabled(enabled);
    verify(profilingSpanProcessor).setEnabled(enabled);
    verifyNoMoreInteractions(spanTracker, traceThreadChangeDetector, profilingSpanProcessor);
  }

  private void assertRuntimeComponentsConfigured() {
    assertThat(stackTraceSamplerSupplier.get()).isInstanceOf(PeriodicStackTraceSampler.class);
    assertThat(stagingAreaSupplier.get()).isInstanceOf(PeriodicallyExportingStagingArea.class);
    assertThat(stackTraceExporterSupplier.get()).isInstanceOf(AsyncStackTraceExporter.class);
  }

  private void assertRuntimeComponentsReset() {
    assertThat(stackTraceSamplerSupplier.get()).isSameAs(StackTraceSampler.NOOP);
    assertThat(stagingAreaSupplier.get()).isSameAs(StagingArea.NOOP);
    assertThat(stackTraceExporterSupplier.get()).isSameAs(StackTraceExporter.NOOP);
  }

  private SnapshotProfilingConfiguration configuration(boolean enabled) {
    return SnapshotProfilingConfiguration.builder()
        .setEnabled(enabled)
        .setConfigProperties(configProperties)
        .build();
  }
}
