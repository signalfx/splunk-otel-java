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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
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
  @Mock ProfilerConfiguration config;
  AutoConfiguredOpenTelemetrySdk sdk;

  ExecutorService executor;
  ProfilingSupervisor supervisor;

  @BeforeEach
  void setUp() {
    executor = Executors.newSingleThreadExecutor();
    sdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .disableShutdownHook()
            .addPropertiesSupplier(
                () ->
                    Map.of(
                        "otel.traces.exporter",
                        "none",
                        "otel.metrics.exporter",
                        "none",
                        "otel.logs.exporter",
                        "none",
                        "otel.service.name",
                        "profiling-supervisor-test"))
            .build();
  }

  @AfterEach
  void tearDown() {
    shutdownRecordingSequencer();
    executor.shutdownNow();
  }

  @Test
  void requestStartDoesNotStartProfilerWhenJfrIsUnavailable() {
    when(jfr.isAvailable()).thenReturn(false);

    supervisor = createSupervisor();

    supervisor.requestStart();

    await().untilAsserted(() -> verify(jfr).isAvailable());
    verify(config, never()).log();
    verify(jfr, never()).setStackDepth(anyInt());
    assertThat(currentSequencer()).isNull();
  }

  @Test
  void requestStartBuildsAndStartsRecordingSequencer(@TempDir Path tempDir) {
    int stackDepth = 4321;
    Duration callStackInterval = Duration.ofMillis(123);
    Duration recordingDuration = Duration.ofDays(1);
    stubStartConfig(tempDir, stackDepth, callStackInterval, recordingDuration, false);
    when(jfr.isAvailable()).thenReturn(true);

    supervisor = createSupervisor();

    supervisor.requestStart();

    RecordingSequencer sequencer = awaitSequencer();
    JfrRecorder recorder = fieldValue(sequencer, "recorder");
    Duration actualRecordingDuration = fieldValue(sequencer, "recordingDuration");
    Duration actualMaxAgeDuration = fieldValue(recorder, "maxAgeDuration");
    JFR actualJfr = fieldValue(recorder, "jfr");
    boolean actualKeepRecordingFiles = fieldValue(recorder, "keepRecordingFiles");
    JfrRecorder actualRecorder = fieldValue(sequencer, "recorder");
    Object recording = fieldValue(recorder, "recording");

    verify(config).log();
    verify(jfr).setStackDepth(stackDepth);
    assertThat(actualRecordingDuration).isEqualTo(recordingDuration);
    assertThat(actualJfr).isSameAs(jfr);
    assertThat(actualMaxAgeDuration).isEqualTo(recordingDuration.multipliedBy(10));
    assertThat(actualKeepRecordingFiles).isFalse();
    assertThat(actualRecorder).isSameAs(recorder);
    assertThat(recording).isNotNull();

    @SuppressWarnings("unchecked")
    Map<String, String> settings = fieldValue(recorder, "settings");
    assertThat(settings).containsEntry("jdk.ThreadDump#period", "123 ms");
  }

  @Test
  void requestStartOnlyStartsOnce(@TempDir Path tempDir) {
    stubStartConfig(tempDir, 1024, Duration.ofSeconds(10), Duration.ofDays(1), false);
    when(jfr.isAvailable()).thenReturn(true);

    supervisor = createSupervisor();

    supervisor.requestStart();
    RecordingSequencer sequencer = awaitSequencer();
    supervisor.requestStart();

    await()
        .during(Duration.ofMillis(200))
        .untilAsserted(() -> assertThat(currentSequencer()).isSameAs(sequencer));
    verify(config).log();
    verify(jfr).setStackDepth(1024);
  }

  @Test
  void requestStartCreatesConfiguredOutputDirectoryWhenKeepingFiles(@TempDir Path tempDir) {
    Path outputDir = tempDir.resolve("profiling-output");
    stubStartConfig(outputDir, 1024, Duration.ZERO, Duration.ofDays(1), true);
    when(jfr.isAvailable()).thenReturn(true);

    supervisor = createSupervisor();

    supervisor.requestStart();

    JfrRecorder recorder = fieldValue(awaitSequencer(), "recorder");
    boolean actualKeepRecordingFiles = fieldValue(recorder, "keepRecordingFiles");
    assertThat(outputDir).isDirectory();
    assertThat(actualKeepRecordingFiles).isTrue();
  }

  @Test
  void requestStartDisablesKeepingFilesWhenOutputPathIsNotDirectory(@TempDir Path tempDir)
      throws Exception {
    Path outputPath = tempDir.resolve("not-a-directory");
    Files.writeString(outputPath, "already a file");
    stubStartConfig(outputPath, 1024, Duration.ZERO, Duration.ofDays(1), true);
    when(jfr.isAvailable()).thenReturn(true);

    supervisor = createSupervisor();

    supervisor.requestStart();

    JfrRecorder recorder = fieldValue(awaitSequencer(), "recorder");
    boolean actualKeepRecordingFiles = fieldValue(recorder, "keepRecordingFiles");
    assertThat(actualKeepRecordingFiles).isFalse();
  }

  private ProfilingSupervisor createSupervisor() {
    return ProfilingSupervisor.createAndStart(config, jfr, executor, sdk);
  }

  private RecordingSequencer awaitSequencer() {
    await().untilAsserted(() -> assertThat(currentSequencer()).isNotNull());
    return currentSequencer();
  }

  private RecordingSequencer currentSequencer() {
    AtomicReference<RecordingSequencer> sequencerReference = fieldValue(supervisor, "sequencer");
    return sequencerReference.get();
  }

  private void shutdownRecordingSequencer() {
    if (supervisor == null) {
      return;
    }
    AtomicReference<RecordingSequencer> sequencerReference = fieldValue(supervisor, "sequencer");
    RecordingSequencer sequencer = sequencerReference.get();
    if (sequencer != null) {
      ScheduledExecutorService sequencerExecutor = fieldValue(sequencer, "executor");
      sequencerExecutor.shutdownNow();
      JfrRecorder recorder = fieldValue(sequencer, "recorder");
      Object recording = fieldValue(recorder, "recording");
      if (recording != null) {
        recorder.stop();
      }
    }
  }

  private void stubStartConfig(
      Path outputDir,
      int stackDepth,
      Duration callStackInterval,
      Duration recordingDuration,
      boolean keepFiles) {
    when(config.getProfilerDirectory()).thenReturn(outputDir.toString());
    when(config.getKeepFiles()).thenReturn(keepFiles);
    when(config.getStackDepth()).thenReturn(stackDepth);
    when(config.getRecordingDuration()).thenReturn(recordingDuration);
    when(config.getCallStackInterval()).thenReturn(callStackInterval);
    when(config.getIncludeAgentInternalStacks()).thenReturn(false);
    when(config.getIncludeJvmInternalStacks()).thenReturn(false);
    when(config.getTracingStacksOnly()).thenReturn(false);
    when(config.getMemoryEnabled()).thenReturn(false);
    when(config.getMemoryEventRateLimitEnabled()).thenReturn(true);
    when(config.getMemoryEventRate()).thenReturn("150/s");
    when(config.getUseAllocationSampleEvent()).thenReturn(false);
    when(config.getConfigProperties())
        .thenReturn(
            DefaultConfigProperties.createFromMap(
                Map.of(
                    "otel.exporter.otlp.protocol",
                    "http/protobuf",
                    "otel.exporter.otlp.endpoint",
                    "http://localhost:4318")));
  }

  @SuppressWarnings("unchecked")
  private static <T> T fieldValue(Object target, String fieldName) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      return (T) field.get(target);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Could not read field " + fieldName + " from " + target, e);
    }
  }
}
