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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
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

  ProfilerConfiguration config;
  OptionalConfigurableSupplier<ProfilerConfiguration> configSupplier;
  TestPeriodicRecordingFlusherFactory recordingFlusherFactory;
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
    recordingFlusherFactory = new TestPeriodicRecordingFlusherFactory();
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
    assertThat(recordingFlusherFactory.createCalled).isFalse();
  }

  @Test
  void requestStartProfiling_buildsAndStartsRecordingFlusher() {
    // given
    when(jfr.isAvailable()).thenReturn(true);

    // when
    supervisor.requestStartProfiling();

    // then
    await().untilAsserted(() -> assertThat(recordingFlusherFactory.createCalled).isTrue());
    verify(config).log();
    assertThat(recordingFlusherFactory.jfr).isSameAs(jfr);
    assertThat(recordingFlusherFactory.config).isSameAs(config);
    assertThat(recordingFlusherFactory.resource).isNotNull();
    verify(recordingFlusherFactory.flusher).start();
  }

  @Test
  void requestStartProfiling_startsOnlyOnce() {
    // given
    when(jfr.isAvailable()).thenReturn(true);

    // when
    supervisor.requestStartProfiling();
    await().untilAsserted(() -> assertThat(recordingFlusherFactory.createCalled).isTrue());
    supervisor.requestStartProfiling();

    // then
    await()
        .during(Duration.ofMillis(200))
        .untilAsserted(() -> verify(recordingFlusherFactory.flusher).start());
    assertThat(recordingFlusherFactory.createCount).isEqualTo(1);
  }

  @Test
  void requestStopProfiling_stopsActiveRecordingFlusher() {
    // given
    when(jfr.isAvailable()).thenReturn(true);
    supervisor.requestStartProfiling();
    await().untilAsserted(() -> verify(recordingFlusherFactory.flusher).start());

    // when
    supervisor.requestStopProfiling();

    // then
    await().untilAsserted(() -> verify(recordingFlusherFactory.flusher).stop());
  }

  @Test
  void requestStartProfiling_startsAfterStop() {
    // given
    when(jfr.isAvailable()).thenReturn(true);
    supervisor.requestStartProfiling();
    await().untilAsserted(() -> verify(recordingFlusherFactory.flusher).start());
    supervisor.requestStopProfiling();
    await().untilAsserted(() -> verify(recordingFlusherFactory.flusher).stop());

    // when
    supervisor.requestStartProfiling();

    // then
    await().untilAsserted(() -> verify(recordingFlusherFactory.flusher, times(2)).start());
    assertThat(recordingFlusherFactory.createCount).isEqualTo(2);
  }

  @Test
  void requestReinitializeProfiling_restartsActiveProfilerWhenEnabled() {
    // given
    when(jfr.isAvailable()).thenReturn(true);
    supervisor.requestStartProfiling();
    await().untilAsserted(() -> verify(recordingFlusherFactory.flusher).start());

    ProfilerConfiguration updatedConfig = spy(config.toBuilder().setStackDepth(1234).build());
    configSupplier.configure(updatedConfig);

    // when
    supervisor.requestReinitializeProfiling();

    // then
    await().untilAsserted(() -> verify(recordingFlusherFactory.flusher, times(2)).start());
    verify(recordingFlusherFactory.flusher).stop();
    verify(updatedConfig).log();
    assertThat(recordingFlusherFactory.createCount).isEqualTo(2);
    assertThat(recordingFlusherFactory.config).isSameAs(updatedConfig);
  }

  @Test
  void requestReinitializeProfiling_stopsActiveProfilerWhenDisabled() {
    // given
    when(jfr.isAvailable()).thenReturn(true);
    supervisor.requestStartProfiling();
    await().untilAsserted(() -> verify(recordingFlusherFactory.flusher).start());

    ProfilerConfiguration disabledConfig = spy(config.toBuilder().setEnabled(false).build());
    configSupplier.configure(disabledConfig);

    // when
    supervisor.requestReinitializeProfiling();

    // then
    await().untilAsserted(() -> verify(recordingFlusherFactory.flusher).stop());
    verify(disabledConfig).isEnabled();
    await()
        .during(Duration.ofMillis(200))
        .untilAsserted(() -> verify(recordingFlusherFactory.flusher).start());
    assertThat(recordingFlusherFactory.createCount).isEqualTo(1);
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
    await().untilAsserted(() -> verify(recordingFlusherFactory.flusher).start());
    verify(recordingFlusherFactory.flusher, never()).stop();
    verify(updatedConfig).log();
    assertThat(recordingFlusherFactory.config).isSameAs(updatedConfig);
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
    verify(recordingFlusherFactory.flusher, never()).start();
    verify(recordingFlusherFactory.flusher, never()).stop();
    assertThat(recordingFlusherFactory.createCalled).isFalse();
  }

  private static class TestPeriodicRecordingFlusherFactory extends PeriodicRecordingFlusherFactory {
    final PeriodicRecordingFlusher flusher = mock(PeriodicRecordingFlusher.class);
    private ProfilerConfiguration config;
    private Resource resource;
    JFR jfr;
    boolean createCalled;
    int createCount;

    @Override
    PeriodicRecordingFlusher create(ProfilerConfiguration config, Resource resource, JFR jfr) {
      this.config = config;
      this.resource = resource;
      this.jfr = jfr;
      createCalled = true;
      createCount++;
      return flusher;
    }
  }
}
