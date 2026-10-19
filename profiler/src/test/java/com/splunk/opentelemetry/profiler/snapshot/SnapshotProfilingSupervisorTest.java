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
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.splunk.opentelemetry.profiler.OtelLoggerFactory;
import com.splunk.opentelemetry.profiler.util.OptionalConfigurableSupplier;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
  @Mock SnapshotProfilerStateListener listener;

  private OptionalConfigurableSupplier<SnapshotProfilingConfiguration> configurationSupplier;
  private ConfigurableSupplier<StackTraceSampler> stackTraceSamplerSupplier;
  private ConfigurableSupplier<StagingArea> stagingAreaSupplier;
  private ConfigurableSupplier<StackTraceExporter> stackTraceExporterSupplier;
  private ExecutorService executor;
  private SnapshotProfilingSupervisor supervisor;
  private MockedStatic<AutoConfigureUtil> autoConfigureUtil;

  @BeforeEach
  void setUp() {
    configurationSupplier = new OptionalConfigurableSupplier<>(configuration(true));

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
    executor = Executors.newSingleThreadExecutor();
    supervisor.start(executor);

    autoConfigureUtil = mockStatic(AutoConfigureUtil.class);
    autoConfigureUtil.when(() -> AutoConfigureUtil.getResource(sdk)).thenReturn(RESOURCE);
  }

  @AfterEach
  void tearDown() {
    supervisor.requestStopProfiling();
    await().untilAsserted(this::assertRuntimeComponentsReset);
    executor.shutdownNow();
    autoConfigureUtil.close();
    Snapshotting.resetProfiling();
  }

  @Test
  void initializeRegistersSupervisor() {
    // given
    SnapshotProfilingConfiguration.SUPPLIER.configure(configuration(true));

    // when
    SnapshotProfilingSupervisor initialized = SnapshotProfilingSupervisor.initialize(sdk);

    // then
    assertThat(SnapshotProfilingSupervisor.SUPPLIER.get()).isSameAs(initialized);
    assertThatThrownBy(() -> SnapshotProfilingSupervisor.initialize(sdk))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Snapshot profiling already initialized");
  }

  @Test
  void startProfilingOnlyOnce() {
    // given
    SnapshotProfilingConfiguration configuration = configuration(true);
    configurationSupplier.configure(configuration);
    registerListenerAndAwaitCurrentConfiguration();
    clearInvocations(listener);

    // when
    requestStartProfiling();
    StackTraceSampler configuredSampler = stackTraceSamplerSupplier.get();
    StagingArea configuredStagingArea = stagingAreaSupplier.get();
    StackTraceExporter configuredExporter = stackTraceExporterSupplier.get();
    supervisor.requestStartProfiling();

    // then
    await()
        .during(Duration.ofMillis(200))
        .untilAsserted(
            () -> {
              verify(profilingSpanProcessor)
                  .setSnapshotSelectionProbability(
                      SnapshotProfilingConfiguration.DEFAULT_SELECTION_PROBABILITY);
              verifyEnabled(true);
              assertThat(stackTraceSamplerSupplier.get()).isSameAs(configuredSampler);
              assertThat(stagingAreaSupplier.get()).isSameAs(configuredStagingArea);
              assertThat(stackTraceExporterSupplier.get()).isSameAs(configuredExporter);
              assertRuntimeComponentsConfigured();
              verify(listener).onSnapshotProfilerStateChanged(configuration);
              verifyNoMoreInteractions(listener);
            });
  }

  @Test
  void stopProfilingOnlyOnce() {
    // given
    SnapshotProfilingConfiguration configuration = configuration(true);
    configurationSupplier.configure(configuration);
    requestStartProfiling();
    await()
        .untilAsserted(
            () -> assertThat(supervisor.getEffectiveConfiguration()).isEqualTo(configuration));
    registerListenerAndAwaitCurrentConfiguration();
    clearInvocations(listener);
    configureRuntimeComponents();
    // Request different settings to check that stopping publishes them with profiling disabled.
    SnapshotProfilingConfiguration requestedConfiguration =
        configuration.toBuilder()
            .setEnabled(false)
            .setSamplingInterval(Duration.ofMillis(20))
            .setSnapshotSelectionProbability(0.5)
            .build();
    configurationSupplier.configure(requestedConfiguration);
    SnapshotProfilingConfiguration disabledConfiguration =
        requestedConfiguration.toBuilder().setEnabled(false).build();

    // when
    supervisor.requestStopProfiling();
    supervisor.requestStopProfiling();

    // then
    // The second stop changes nothing, so there should be just one disabled notification.
    await()
        .during(Duration.ofMillis(200))
        .untilAsserted(
            () -> verify(listener).onSnapshotProfilerStateChanged(disabledConfiguration));
    verifyNoMoreInteractions(listener);
    verifyClosedRuntimeComponents();
    verify(spanTracker).setEnabled(true);
    verify(spanTracker).setEnabled(false);
    verify(traceThreadChangeDetector).setEnabled(true);
    verify(traceThreadChangeDetector).setEnabled(false);
    verify(profilingSpanProcessor).setEnabled(true);
    verify(profilingSpanProcessor).setEnabled(false);
    verify(profilingSpanProcessor)
        .setSnapshotSelectionProbability(
            SnapshotProfilingConfiguration.DEFAULT_SELECTION_PROBABILITY);
    verifyNoMoreInteractions(spanTracker, traceThreadChangeDetector, profilingSpanProcessor);
    assertRuntimeComponentsReset();
    assertThat(supervisor.isRunning()).isFalse();
    assertThat(supervisor.getEffectiveConfiguration()).isEqualTo(disabledConfiguration);
  }

  @Test
  void doNotStartProfilingWhenReinitializedWithDisabledConfiguration() {
    // given
    configurationSupplier.configure(configuration(false));

    // when
    supervisor.requestReinitializeProfiling();

    // then
    await()
        .during(Duration.ofMillis(200))
        .untilAsserted(
            () -> {
              verifyNoInteractions(spanTracker, traceThreadChangeDetector, profilingSpanProcessor);
              assertRuntimeComponentsReset();
            });
  }

  @Test
  void reportsDisabledBeforeStartupAndAfterStartupFailure() {
    // given
    configurationSupplier.configure(configuration(true));
    // Registration reports disabled even though the requested configuration enables profiling.
    registerListenerAndAwaitCurrentConfiguration();
    verify(listener).onSnapshotProfilerStateChanged(configuration(false));
    // Clear the initial callback to check that a failed start sends no duplicate notification.
    clearInvocations(listener);
    assertThat(supervisor.getEffectiveConfiguration().isEnabled()).isFalse();
    // Fail the first start attempt and allow the retry to succeed.
    doThrow(new IllegalStateException("Cannot enable snapshot profiling"))
        .doNothing()
        .when(profilingSpanProcessor)
        .setEnabled(true);
    doNothing().when(profilingSpanProcessor).setEnabled(false);

    // when
    supervisor.requestStartProfiling();

    // then
    await().untilAsserted(() -> verify(profilingSpanProcessor).setEnabled(false));
    assertThat(supervisor.getEffectiveConfiguration().isEnabled()).isFalse();
    assertRuntimeComponentsReset();
    verifyNoInteractions(listener);

    // when: retry startup after the failure
    supervisor.requestStartProfiling();

    // then
    await()
        .untilAsserted(() -> verify(listener).onSnapshotProfilerStateChanged(configuration(true)));
    assertThat(supervisor.getEffectiveConfiguration()).isEqualTo(configuration(true));
  }

  @Test
  void reportsDisabledUntilStartupCompletes() throws InterruptedException {
    // given
    CountDownLatch starting = new CountDownLatch(1);
    CountDownLatch finishStartup = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              starting.countDown();
              finishStartup.await(5, TimeUnit.SECONDS);
              return null;
            })
        .when(profilingSpanProcessor)
        .setEnabled(true);

    // when
    supervisor.requestStartProfiling();

    // then: creating components does not yet make profiling effective
    try {
      assertThat(starting.await(5, TimeUnit.SECONDS)).isTrue();
      assertRuntimeComponentsConfigured();
      assertThat(supervisor.isRunning()).isFalse();
      assertThat(supervisor.getEffectiveConfiguration()).isEqualTo(configuration(false));
    } finally {
      finishStartup.countDown();
    }
    await()
        .untilAsserted(
            () ->
                assertThat(supervisor.getEffectiveConfiguration()).isEqualTo(configuration(true)));
  }

  @Test
  void reportsUpdatedDisabledSettingsWhenStandaloneStartFails() {
    // given
    registerListenerAndAwaitCurrentConfiguration();
    clearInvocations(listener);
    doThrow(new IllegalStateException("Cannot start snapshot profiling"))
        .when(profilingSpanProcessor)
        .setEnabled(true);
    SnapshotProfilingConfiguration updatedConfiguration =
        configuration(true).toBuilder()
            .setSamplingInterval(Duration.ofMillis(20))
            .setSnapshotSelectionProbability(0.5)
            .build();

    // when
    configurationSupplier.configure(updatedConfiguration);

    // then: requested settings remain unapplied until startup is attempted
    assertThat(supervisor.getEffectiveConfiguration()).isEqualTo(configuration(false));

    // when
    supervisor.requestStartProfiling();

    // then
    SnapshotProfilingConfiguration disabledUpdatedConfiguration =
        updatedConfiguration.toBuilder().setEnabled(false).build();
    await()
        .untilAsserted(
            () -> {
              verify(listener).onSnapshotProfilerStateChanged(disabledUpdatedConfiguration);
              verifyNoMoreInteractions(listener);
            });
    verify(profilingSpanProcessor).setEnabled(false);
    assertRuntimeComponentsReset();
    assertThat(supervisor.isRunning()).isFalse();
    assertThat(supervisor.getEffectiveConfiguration()).isEqualTo(disabledUpdatedConfiguration);
  }

  @Test
  void startProfilingOverridesDisabledRequestedConfiguration() {
    // given
    configurationSupplier.configure(configuration(false));
    registerListenerAndAwaitCurrentConfiguration();
    clearInvocations(listener);

    // when
    requestStartProfiling();

    // then
    await()
        .untilAsserted(() -> verify(listener).onSnapshotProfilerStateChanged(configuration(true)));
    verifyNoMoreInteractions(listener);
    assertThat(supervisor.getEffectiveConfiguration()).isEqualTo(configuration(true));
  }

  @Test
  void reportsAppliedSettingsUntilReinitializationCompletes() {
    // given
    SnapshotProfilingConfiguration initialConfiguration = configuration(true);
    configurationSupplier.configure(initialConfiguration);
    registerListenerAndAwaitCurrentConfiguration();
    requestStartProfiling();
    await()
        .untilAsserted(() -> verify(listener).onSnapshotProfilerStateChanged(initialConfiguration));
    clearInvocations(listener);
    StackTraceSampler initialSampler = stackTraceSamplerSupplier.get();
    StagingArea initialStagingArea = stagingAreaSupplier.get();
    StackTraceExporter initialExporter = stackTraceExporterSupplier.get();
    configureRuntimeComponents();
    clearInvocations(spanTracker, traceThreadChangeDetector, profilingSpanProcessor);
    SnapshotProfilingConfiguration updatedConfiguration =
        initialConfiguration.toBuilder()
            .setSamplingInterval(Duration.ofMillis(20))
            .setSnapshotSelectionProbability(0.5)
            .setStackDepth(512)
            .build();

    // when
    configurationSupplier.configure(updatedConfiguration);

    // then: the profiler still reports the settings it is currently using
    assertThat(supervisor.getEffectiveConfiguration()).isEqualTo(initialConfiguration);

    // when: reinitialize to apply the new settings
    supervisor.requestReinitializeProfiling();

    // then
    await()
        .untilAsserted(
            () -> {
              verify(listener).onSnapshotProfilerStateChanged(updatedConfiguration);
              verifyClosedRuntimeComponents();
              verify(spanTracker).setEnabled(false);
              verify(spanTracker).setEnabled(true);
              verify(traceThreadChangeDetector).setEnabled(false);
              verify(traceThreadChangeDetector).setEnabled(true);
              verify(profilingSpanProcessor).setEnabled(false);
              verify(profilingSpanProcessor).setSnapshotSelectionProbability(0.5);
              verify(profilingSpanProcessor).setEnabled(true);
              verifyNoMoreInteractions(
                  spanTracker, traceThreadChangeDetector, profilingSpanProcessor);
              verifyNoMoreInteractions(listener);
            });
    assertThat(stackTraceSamplerSupplier.get()).isNotSameAs(initialSampler);
    assertThat(stagingAreaSupplier.get()).isNotSameAs(initialStagingArea);
    assertThat(stackTraceExporterSupplier.get()).isNotSameAs(initialExporter);
    assertRuntimeComponentsConfigured();
    assertThat(supervisor.getEffectiveConfiguration()).isEqualTo(updatedConfiguration);

    // when: restart again with the same settings
    clearInvocations(listener);
    supervisor.requestReinitializeProfiling();

    // then: the unchanged final configuration produces no notification
    await()
        .during(Duration.ofMillis(200))
        .untilAsserted(
            () -> {
              verify(profilingSpanProcessor, times(2)).setEnabled(true);
              assertThat(supervisor.getEffectiveConfiguration()).isEqualTo(updatedConfiguration);
              verifyNoInteractions(listener);
            });
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void reportsUpdatedDisabledSettingsAfterReinitializationFailure(boolean previouslyStarted) {
    // given
    configurationSupplier.configure(configuration(true));
    registerListenerAndAwaitCurrentConfiguration();
    if (previouslyStarted) {
      requestStartProfiling();
      await()
          .untilAsserted(
              () -> verify(listener).onSnapshotProfilerStateChanged(configuration(true)));
    }
    clearInvocations(listener);
    doThrow(new IllegalStateException("Cannot restart snapshot profiling"))
        .when(profilingSpanProcessor)
        .setEnabled(true);
    doNothing().when(profilingSpanProcessor).setEnabled(false);
    SnapshotProfilingConfiguration updatedConfiguration =
        configuration(true).toBuilder()
            .setSamplingInterval(Duration.ofMillis(20))
            .setSnapshotSelectionProbability(0.5)
            .build();
    configurationSupplier.configure(updatedConfiguration);
    SnapshotProfilingConfiguration disabledUpdatedConfiguration =
        updatedConfiguration.toBuilder().setEnabled(false).build();

    // when
    supervisor.requestReinitializeProfiling();

    // then
    // Wait for cleanup of the previous profiler and the failed restart.
    await()
        .untilAsserted(
            () -> {
              verify(profilingSpanProcessor, times(previouslyStarted ? 2 : 1)).setEnabled(false);
              verify(listener).onSnapshotProfilerStateChanged(disabledUpdatedConfiguration);
              verifyNoMoreInteractions(listener);
            });
    assertThat(supervisor.getEffectiveConfiguration()).isEqualTo(disabledUpdatedConfiguration);
    assertRuntimeComponentsReset();
  }

  @Test
  void registeringListenerDeliversCurrentEffectiveConfiguration() {
    // given
    configurationSupplier.configure(configuration(true));

    // when: register before profiling starts
    registerListenerAndAwaitCurrentConfiguration();

    // then
    verify(listener).onSnapshotProfilerStateChanged(configuration(false));

    // given: profiling is now running
    requestStartProfiling();
    await()
        .untilAsserted(() -> verify(listener).onSnapshotProfilerStateChanged(configuration(true)));
    supervisor.removeSnapshotProfilerStateListener(listener);
    clearInvocations(listener);

    // when: register again while profiling is running
    registerListenerAndAwaitCurrentConfiguration();

    // then
    verify(listener).onSnapshotProfilerStateChanged(configuration(true));
  }

  @Test
  void stopProfilingWhenReinitializedWithDisabledConfiguration() {
    // given
    SnapshotProfilingConfiguration initialConfiguration = configuration(true);
    configurationSupplier.configure(initialConfiguration);
    requestStartProfiling();
    configureRuntimeComponents();
    clearInvocations(spanTracker, traceThreadChangeDetector, profilingSpanProcessor);

    SnapshotProfilingConfiguration disabledConfiguration = configuration(false);
    configurationSupplier.configure(disabledConfiguration);

    // when
    supervisor.requestReinitializeProfiling();

    // then
    await()
        .untilAsserted(
            () -> {
              verifyClosedRuntimeComponents();
              verifyEnabled(false);
              assertRuntimeComponentsReset();
            });
  }

  private void registerListenerAndAwaitCurrentConfiguration() {
    SnapshotProfilingConfiguration currentConfiguration = supervisor.getEffectiveConfiguration();
    supervisor.addSnapshotProfilerStateListener(listener);
    await()
        .untilAsserted(() -> verify(listener).onSnapshotProfilerStateChanged(currentConfiguration));
  }

  private void requestStartProfiling() {
    supervisor.requestStartProfiling();
    await()
        .untilAsserted(
            () -> {
              assertRuntimeComponentsConfigured();
              assertThat(supervisor.isRunning()).isTrue();
            });
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
