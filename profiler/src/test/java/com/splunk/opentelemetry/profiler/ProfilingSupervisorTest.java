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

package com.splunk.opentelemetry.profiler;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.instrumentation.jvmmetrics.otel.OtelAllocatedMemoryMetrics;
import com.splunk.opentelemetry.instrumentation.jvmmetrics.otel.OtelGcMemoryMetrics;
import com.splunk.opentelemetry.profiler.util.OptionalConfigurableSupplier;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfilingSupervisorTest {
  private static final String JVM_METRICS_ENABLED_CONFIG_KEY =
      "otel.instrumentation.jvm-metrics-splunk.enabled";

  @Mock JFR jfr;
  @Mock PeriodicRecordingFlusherFactory recordingFlusherFactory;
  @Mock PeriodicRecordingFlusher recordingFlusher;
  @Mock OtelAllocatedMemoryMetrics allocatedMemoryMetrics;
  @Mock OtelGcMemoryMetrics gcMemoryMetrics;

  ProfilerConfiguration config;
  OptionalConfigurableSupplier<ProfilerConfiguration> configSupplier;
  ExecutorService executor;
  ProfilingSupervisor supervisor;

  @BeforeEach
  void setUp(@TempDir Path tempDir) {
    config =
        ProfilerConfiguration.builder()
            .setEnabled(true)
            .setProfilerDirectory(tempDir.toString())
            .setStackDepth(4321)
            .setRecordingDuration(Duration.ofMinutes(1))
            .build();
    configSupplier = new OptionalConfigurableSupplier<>();
    configSupplier.configure(config);
    lenient()
        .when(
            recordingFlusherFactory.create(
                any(ProfilerConfiguration.class), any(Resource.class), any(JFR.class)))
        .thenReturn(recordingFlusher);
  }

  @AfterEach
  void tearDown() {
    executor.shutdownNow();
  }

  @Test
  void requestStartProfiling_doesNotStartProfilerWhenJfrIsUnavailable() {
    // given
    startSupervisor();
    when(jfr.isAvailable()).thenReturn(false);

    // when
    supervisor.requestStartProfiling();

    // then
    await().untilAsserted(() -> verify(jfr).isAvailable());
    verify(jfr, never()).setStackDepth(anyInt());
    verify(recordingFlusherFactory, never()).create(any(), any(), any());
  }

  @Test
  void requestStartProfiling_buildsAndStartsRecordingFlusher() {
    // given
    startSupervisor();
    when(jfr.isAvailable()).thenReturn(true);

    // when
    supervisor.requestStartProfiling();

    // then
    await().untilAsserted(() -> verify(recordingFlusher).start());
    verifyRecordingFlusherCreatedWith(config);
  }

  @Test
  void requestStartProfiling_startsOnlyOnce() {
    // given
    startSupervisor();
    when(jfr.isAvailable()).thenReturn(true);

    // when
    supervisor.requestStartProfiling();
    await().untilAsserted(() -> verify(recordingFlusherFactory).create(any(), any(), any()));
    supervisor.requestStartProfiling();

    // then
    await()
        .during(Duration.ofMillis(200))
        .untilAsserted(
            () -> {
              verify(recordingFlusher).start();
              verifyRecordingFlusherCreated(1);
            });
  }

  @Test
  void requestStopProfiling_stopsActiveRecordingFlusher() {
    // given
    startSupervisor();
    when(jfr.isAvailable()).thenReturn(true);
    supervisor.requestStartProfiling();
    await().untilAsserted(() -> verify(recordingFlusher).start());

    // when
    supervisor.requestStopProfiling();

    // then
    await().untilAsserted(() -> verify(recordingFlusher).stop());
  }

  @Test
  void requestStartProfiling_startsAfterStop() {
    // given
    startSupervisor();
    when(jfr.isAvailable()).thenReturn(true);
    supervisor.requestStartProfiling();
    await().untilAsserted(() -> verify(recordingFlusher).start());
    supervisor.requestStopProfiling();
    await().untilAsserted(() -> verify(recordingFlusher).stop());

    // when
    supervisor.requestStartProfiling();

    // then
    await().untilAsserted(() -> verify(recordingFlusher, times(2)).start());
    verifyRecordingFlusherCreated(2);
  }

  @Test
  void requestReinitializeProfiling_restartsActiveProfilerWhenEnabled() {
    // given
    startSupervisor();
    when(jfr.isAvailable()).thenReturn(true);
    supervisor.requestStartProfiling();
    await().untilAsserted(() -> verify(recordingFlusher).start());

    ProfilerConfiguration updatedConfig = config.toBuilder().setStackDepth(1234).build();
    configSupplier.configure(updatedConfig);

    // when
    supervisor.requestReinitializeProfiling();

    // then
    await().untilAsserted(() -> verify(recordingFlusher, times(2)).start());
    verify(recordingFlusher).stop();
    verifyRecordingFlusherCreated(2);
    verifyRecordingFlusherCreatedWith(updatedConfig);
  }

  @Test
  void requestReinitializeProfiling_stopsActiveProfilerWhenDisabled() {
    // given
    startSupervisor();
    when(jfr.isAvailable()).thenReturn(true);
    supervisor.requestStartProfiling();
    await().untilAsserted(() -> verify(recordingFlusher).start());

    ProfilerConfiguration disabledConfig = config.toBuilder().setEnabled(false).build();
    configSupplier.configure(disabledConfig);

    // when
    supervisor.requestReinitializeProfiling();

    // then
    await().untilAsserted(() -> verify(recordingFlusher).stop());
    await()
        .during(Duration.ofMillis(200))
        .untilAsserted(
            () -> {
              verify(recordingFlusher).start();
              verifyRecordingFlusherCreated(1);
            });
  }

  @Test
  void requestReinitializeProfiling_startsInactiveProfilerWhenEnabled() {
    // given
    startSupervisor();
    when(jfr.isAvailable()).thenReturn(true);
    ProfilerConfiguration updatedConfig = config.toBuilder().setStackDepth(1234).build();
    configSupplier.configure(updatedConfig);

    // when
    supervisor.requestReinitializeProfiling();

    // then
    await().untilAsserted(() -> verify(recordingFlusher).start());
    verify(recordingFlusher, never()).stop();
    verifyRecordingFlusherCreatedWith(updatedConfig);
  }

  @Test
  void requestReinitializeProfiling_keepsInactiveProfilerStoppedWhenDisabled() {
    // given
    startSupervisor();
    ProfilerConfiguration disabledConfig = config.toBuilder().setEnabled(false).build();
    configSupplier.configure(disabledConfig);

    // when
    supervisor.requestReinitializeProfiling();

    // then
    verify(jfr, never()).isAvailable();
    verify(recordingFlusher, never()).start();
    verify(recordingFlusher, never()).stop();
    verify(recordingFlusherFactory, never()).create(any(), any(), any());
  }

  @Test
  void requestReinitializeProfiling_installsJvmMemoryMetricsWhenMemoryProfilerIsEnabled() {
    // given
    startSupervisor();
    ProfilerConfiguration memoryProfilerConfig =
        config.toBuilder().setEnabled(false).setMemoryEnabled(true).build();
    configSupplier.configure(memoryProfilerConfig);

    // when
    supervisor.requestReinitializeProfiling();

    // then
    await()
        .untilAsserted(
            () -> {
              verify(allocatedMemoryMetrics).install();
              verify(gcMemoryMetrics).install();
            });
    verify(recordingFlusherFactory, never()).create(any(), any(), any());
  }

  @Test
  void requestReinitializeProfiling_uninstallsJvmMemoryMetricsWhenMemoryProfilerIsDisabled() {
    // given
    startSupervisor();
    ProfilerConfiguration disabledMemoryProfilerConfig =
        config.toBuilder().setEnabled(false).setMemoryEnabled(false).build();
    configSupplier.configure(disabledMemoryProfilerConfig);

    // when
    supervisor.requestReinitializeProfiling();

    // then
    await()
        .untilAsserted(
            () -> {
              verify(allocatedMemoryMetrics).uninstall();
              verify(gcMemoryMetrics).uninstall();
            });
    verify(recordingFlusherFactory, never()).create(any(), any(), any());
  }

  @Test
  void requestReinitializeProfiling_doesNotInstallJvmMemoryMetricsWhenDisabledByProperty() {
    // given
    startSupervisor(createSdk(Map.of(JVM_METRICS_ENABLED_CONFIG_KEY, "false")));
    ProfilerConfiguration memoryProfilerConfig =
        config.toBuilder().setEnabled(false).setMemoryEnabled(true).build();
    configSupplier.configure(memoryProfilerConfig);

    // when
    supervisor.requestReinitializeProfiling();

    // then
    await()
        .untilAsserted(
            () -> {
              verify(allocatedMemoryMetrics).uninstall();
              verify(gcMemoryMetrics).uninstall();
            });
    verify(allocatedMemoryMetrics, never()).install();
    verify(gcMemoryMetrics, never()).install();
    verify(recordingFlusherFactory, never()).create(any(), any(), any());
  }

  private AutoConfiguredOpenTelemetrySdk createSdk(Map<String, String> properties) {
    Map<String, String> allProperties = new HashMap<>();
    allProperties.put("otel.traces.exporter", "none");
    allProperties.put("otel.metrics.exporter", "none");
    allProperties.put("otel.logs.exporter", "none");
    allProperties.put("otel.service.name", "profiling-supervisor-test");
    allProperties.putAll(properties);

    return AutoConfiguredOpenTelemetrySdk.builder()
        .disableShutdownHook()
        .addPropertiesSupplier(() -> allProperties)
        .build();
  }

  private void startSupervisor(AutoConfiguredOpenTelemetrySdk sdk) {
    executor = Executors.newSingleThreadExecutor();
    supervisor =
        new ProfilingSupervisor(
            configSupplier,
            jfr,
            sdk,
            new LinkedBlockingQueue<>(),
            recordingFlusherFactory,
            allocatedMemoryMetrics,
            gcMemoryMetrics);
    supervisor.start(executor);
  }

  private void startSupervisor() {
    startSupervisor(createSdk(Map.of()));
  }

  private void verifyRecordingFlusherCreated(int count) {
    verify(recordingFlusherFactory, times(count)).create(any(), any(), any());
  }

  private void verifyRecordingFlusherCreatedWith(ProfilerConfiguration config) {
    verify(recordingFlusherFactory).create(same(config), any(Resource.class), same(jfr));
  }
}
