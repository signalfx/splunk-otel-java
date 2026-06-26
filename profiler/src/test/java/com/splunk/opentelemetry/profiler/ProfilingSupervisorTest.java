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
  TestPeriodicRecordingFlusherBuilder builder;
  AutoConfiguredOpenTelemetrySdk sdk;
  ExecutorService executor;
  TestProfilingSupervisor supervisor;

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
    builder = new TestPeriodicRecordingFlusherBuilder(config, mock(Resource.class));
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
    supervisor = new TestProfilingSupervisor(config, jfr, sdk, builder);
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
    assertThat(builder.buildCalled).isFalse();
  }

  @Test
  void requestStartProfiling_buildsAndStartsRecordingFlusher() {
    // given
    when(jfr.isAvailable()).thenReturn(true);

    // when
    supervisor.requestStartProfiling();

    // then
    await().untilAsserted(() -> assertThat(builder.buildCalled).isTrue());
    verify(config).log();
    assertThat(builder.jfr).isSameAs(jfr);
    assertThat(builder.config).isSameAs(config);
    assertThat(builder.resource).isNotNull();
    verify(builder.flusher).start();
  }

  @Test
  void requestStartProfiling_startsOnlyOnce() {
    // given
    when(jfr.isAvailable()).thenReturn(true);

    // when
    supervisor.requestStartProfiling();
    await().untilAsserted(() -> assertThat(builder.buildCalled).isTrue());
    supervisor.requestStartProfiling();

    // then
    await().during(Duration.ofMillis(200)).untilAsserted(() -> verify(builder.flusher).start());
    assertThat(builder.buildCount).isEqualTo(1);
  }

  @Test
  void requestStopProfiling_stopsActiveRecordingFlusher() {
    // given
    when(jfr.isAvailable()).thenReturn(true);
    supervisor.requestStartProfiling();
    await().untilAsserted(() -> verify(builder.flusher).start());

    // when
    supervisor.requestStopProfiling();

    // then
    await().untilAsserted(() -> verify(builder.flusher).stop());
  }

  @Test
  void requestStartProfiling_startsAfterStop() {
    // given
    when(jfr.isAvailable()).thenReturn(true);
    supervisor.requestStartProfiling();
    await().untilAsserted(() -> verify(builder.flusher).start());
    supervisor.requestStopProfiling();
    await().untilAsserted(() -> verify(builder.flusher).stop());

    // when
    supervisor.requestStartProfiling();

    // then
    await().untilAsserted(() -> verify(builder.flusher, times(2)).start());
    assertThat(builder.buildCount).isEqualTo(2);
  }

  @Test
  void requestReinitializeProfiling_restartsActiveProfilerWhenEnabled() {
    // given
    when(jfr.isAvailable()).thenReturn(true);
    supervisor.requestStartProfiling();
    await().untilAsserted(() -> verify(builder.flusher).start());

    ProfilerConfiguration updatedConfig = spy(config.toBuilder().setStackDepth(1234).build());
    supervisor.configureProfilerConfiguration(updatedConfig);

    // when
    supervisor.requestReinitializeProfiling();

    // then
    await().untilAsserted(() -> verify(builder.flusher, times(2)).start());
    verify(builder.flusher).stop();
    verify(updatedConfig).log();
    assertThat(builder.buildCount).isEqualTo(2);
    assertThat(builder.config).isSameAs(updatedConfig);
  }

  @Test
  void requestReinitializeProfiling_stopsActiveProfilerWhenDisabled() {
    // given
    when(jfr.isAvailable()).thenReturn(true);
    supervisor.requestStartProfiling();
    await().untilAsserted(() -> verify(builder.flusher).start());

    ProfilerConfiguration disabledConfig = spy(config.toBuilder().setEnabled(false).build());
    supervisor.configureProfilerConfiguration(disabledConfig);

    // when
    supervisor.requestReinitializeProfiling();

    // then
    await().untilAsserted(() -> verify(builder.flusher).stop());
    verify(disabledConfig).isEnabled();
    await().during(Duration.ofMillis(200)).untilAsserted(() -> verify(builder.flusher).start());
    assertThat(builder.buildCount).isEqualTo(1);
  }

  @Test
  void requestReinitializeProfiling_startsInactiveProfilerWhenEnabled() {
    // given
    when(jfr.isAvailable()).thenReturn(true);
    ProfilerConfiguration updatedConfig = spy(config.toBuilder().setStackDepth(1234).build());
    supervisor.configureProfilerConfiguration(updatedConfig);

    // when
    supervisor.requestReinitializeProfiling();

    // then
    await().untilAsserted(() -> verify(builder.flusher).start());
    verify(builder.flusher, never()).stop();
    verify(updatedConfig).log();
    assertThat(builder.config).isSameAs(updatedConfig);
  }

  @Test
  void requestReinitializeProfiling_keepsInactiveProfilerStoppedWhenDisabled() {
    // given
    ProfilerConfiguration disabledConfig = spy(config.toBuilder().setEnabled(false).build());
    supervisor.configureProfilerConfiguration(disabledConfig);

    // when
    supervisor.requestReinitializeProfiling();

    // then
    await().untilAsserted(() -> verify(disabledConfig).isEnabled());
    verify(jfr, never()).isAvailable();
    verify(builder.flusher, never()).start();
    verify(builder.flusher, never()).stop();
    assertThat(builder.buildCalled).isFalse();
  }

  private static class TestProfilingSupervisor extends ProfilingSupervisor {
    private final TestPeriodicRecordingFlusherBuilder builder;
    private final OptionalConfigurableSupplier<ProfilerConfiguration> configSupplier;

    TestProfilingSupervisor(
        ProfilerConfiguration config,
        JFR jfr,
        AutoConfiguredOpenTelemetrySdk sdk,
        TestPeriodicRecordingFlusherBuilder builder) {
      this(configSupplier(config), jfr, sdk, builder);
    }

    private TestProfilingSupervisor(
        OptionalConfigurableSupplier<ProfilerConfiguration> configSupplier,
        JFR jfr,
        AutoConfiguredOpenTelemetrySdk sdk,
        TestPeriodicRecordingFlusherBuilder builder) {
      super(configSupplier, jfr, sdk, new LinkedBlockingQueue<>());
      this.builder = builder;
      this.configSupplier = configSupplier;
    }

    void configureProfilerConfiguration(ProfilerConfiguration config) {
      configSupplier.configure(config);
    }

    @Override
    PeriodicRecordingFlusherBuilder makeRecordingFlusherBuilder(Resource resource) {
      return builder.withConfigAndResource(configSupplier.get(), resource);
    }

    private static OptionalConfigurableSupplier<ProfilerConfiguration> configSupplier(
        ProfilerConfiguration config) {
      OptionalConfigurableSupplier<ProfilerConfiguration> supplier =
          new OptionalConfigurableSupplier<>();
      supplier.configure(config);
      return supplier;
    }
  }

  private static class TestPeriodicRecordingFlusherBuilder extends PeriodicRecordingFlusherBuilder {
    final PeriodicRecordingFlusher flusher = mock(PeriodicRecordingFlusher.class);
    private ProfilerConfiguration config;
    private Resource resource;
    JFR jfr;
    boolean buildCalled;
    int buildCount;

    public TestPeriodicRecordingFlusherBuilder(ProfilerConfiguration config, Resource resource) {
      super(config, resource);
      this.config = config;
      this.resource = resource;
    }

    TestPeriodicRecordingFlusherBuilder withConfigAndResource(
        ProfilerConfiguration config, Resource resource) {
      this.config = config;
      this.resource = resource;
      return this;
    }

    @Override
    PeriodicRecordingFlusherBuilder jfr(JFR jfr) {
      this.jfr = jfr;
      return this;
    }

    @Override
    PeriodicRecordingFlusher build() {
      buildCalled = true;
      buildCount++;
      return flusher;
    }
  }
}
