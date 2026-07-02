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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.profiler.util.OptionalConfigurableSupplier;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import java.nio.file.Path;
import java.time.Duration;
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

  @Mock JFR jfr;
  @Mock PeriodicRecordingFlusherFactory recordingFlusherFactory;
  @Mock PeriodicRecordingFlusher recordingFlusher;

  ProfilerConfiguration config;
  OptionalConfigurableSupplier<ProfilerConfiguration> configSupplier;
  AutoConfiguredOpenTelemetrySdk sdk;
  ExecutorService executor;
  ProfilingSupervisor supervisor;

  @BeforeEach
  void setUp(@TempDir Path tempDir) {
    config =
        spy(
            ProfilerConfiguration.builder()
                .setEnabled(true)
                .setProfilerDirectory(tempDir.toString())
                .setStackDepth(4321)
                .setRecordingDuration(Duration.ofMinutes(1))
                .build());
    configSupplier = new OptionalConfigurableSupplier<>();
    configSupplier.configure(config);
    lenient()
        .when(
            recordingFlusherFactory.create(
                any(ProfilerConfiguration.class), any(Resource.class), any(JFR.class)))
        .thenReturn(recordingFlusher);
    executor = Executors.newSingleThreadExecutor();
    sdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .disableShutdownHook()
            .addPropertiesSupplier(
                () ->
                    java.util.Map.of(
                        "otel.traces.exporter",
                        "none",
                        "otel.metrics.exporter",
                        "none",
                        "otel.logs.exporter",
                        "none",
                        "otel.service.name",
                        "profiling-supervisor-test"))
            .build();
    supervisor =
        new ProfilingSupervisor(
            configSupplier, jfr, sdk, new LinkedBlockingQueue<>(), recordingFlusherFactory);
    supervisor.start(executor);
  }

  @AfterEach
  void tearDown() {
    executor.shutdownNow();
  }

  @Test
  void requestStartProfiling_doesNotStartProfilerWhenJfrIsUnavailable() {
    // given
    when(jfr.isAvailable()).thenReturn(false);

    // when
    supervisor.requestStartProfiling();

    // then
    await().untilAsserted(() -> verify(jfr).isAvailable());
    verify(config, never()).log();
    verify(jfr, never()).setStackDepth(anyInt());
    verify(recordingFlusherFactory, never()).create(any(), any(), any());
  }

  @Test
  void requestStartProfiling_buildsAndStartsRecordingFlusher() {
    // given
    when(jfr.isAvailable()).thenReturn(true);

    // when
    supervisor.requestStartProfiling();

    // then
    await().untilAsserted(() -> verify(recordingFlusher).start());
    verify(config).log();
    verifyRecordingFlusherCreatedWith(config);
  }

  @Test
  void requestStartProfiling_startsOnlyOnce() {
    // given
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
    when(jfr.isAvailable()).thenReturn(true);
    supervisor.requestStartProfiling();
    await().untilAsserted(() -> verify(recordingFlusher).start());

    ProfilerConfiguration updatedConfig = spy(config.toBuilder().setStackDepth(1234).build());
    configSupplier.configure(updatedConfig);

    // when
    supervisor.requestReinitializeProfiling();

    // then
    await().untilAsserted(() -> verify(recordingFlusher, times(2)).start());
    verify(recordingFlusher).stop();
    verify(updatedConfig).log();
    verifyRecordingFlusherCreated(2);
    verifyRecordingFlusherCreatedWith(updatedConfig);
  }

  @Test
  void requestReinitializeProfiling_stopsActiveProfilerWhenDisabled() {
    // given
    when(jfr.isAvailable()).thenReturn(true);
    supervisor.requestStartProfiling();
    await().untilAsserted(() -> verify(recordingFlusher).start());

    ProfilerConfiguration disabledConfig = spy(config.toBuilder().setEnabled(false).build());
    configSupplier.configure(disabledConfig);

    // when
    supervisor.requestReinitializeProfiling();

    // then
    await().untilAsserted(() -> verify(recordingFlusher).stop());
    verify(disabledConfig).isEnabled();
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
    when(jfr.isAvailable()).thenReturn(true);
    ProfilerConfiguration updatedConfig = spy(config.toBuilder().setStackDepth(1234).build());
    configSupplier.configure(updatedConfig);

    // when
    supervisor.requestReinitializeProfiling();

    // then
    await().untilAsserted(() -> verify(recordingFlusher).start());
    verify(recordingFlusher, never()).stop();
    verify(updatedConfig).log();
    verifyRecordingFlusherCreatedWith(updatedConfig);
  }

  @Test
  void requestReinitializeProfiling_keepsInactiveProfilerStoppedWhenDisabled() {
    // given
    ProfilerConfiguration disabledConfig = spy(config.toBuilder().setEnabled(false).build());
    configSupplier.configure(disabledConfig);

    // when
    supervisor.requestReinitializeProfiling();

    // then
    await().untilAsserted(() -> verify(disabledConfig).isEnabled());
    verify(jfr, never()).isAvailable();
    verify(recordingFlusher, never()).start();
    verify(recordingFlusher, never()).stop();
    verify(recordingFlusherFactory, never()).create(any(), any(), any());
  }

  private void verifyRecordingFlusherCreated(int count) {
    verify(recordingFlusherFactory, times(count)).create(any(), any(), any());
  }

  private void verifyRecordingFlusherCreatedWith(ProfilerConfiguration config) {
    verify(recordingFlusherFactory).create(same(config), any(Resource.class), same(jfr));
  }
}
