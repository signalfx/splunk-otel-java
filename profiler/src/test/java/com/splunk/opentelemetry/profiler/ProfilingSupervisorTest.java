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
  TestProfilingConfig config;
  AutoConfiguredOpenTelemetrySdk sdk;

  ExecutorService executor;
  ProfilingSupervisor supervisor;

  @BeforeEach
  void setUp() {
    executor = Executors.newSingleThreadExecutor();
    config = new TestProfilingConfig();
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
    assertThat(config.logCalled).isFalse();
    verify(jfr, never()).setStackDepth(anyInt());
    assertThat(currentSequencer()).isNull();
  }

  @Test
  void requestStartBuildsAndStartsRecordingSequencer(@TempDir Path tempDir) {
    config.profilerDirectory = tempDir.toString();
    config.stackDepth = 4321;
    config.callStackInterval = Duration.ofMillis(123);
    config.recordingDuration = Duration.ofMinutes(1);
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

    assertThat(config.logCalled).isTrue();
    verify(jfr).setStackDepth(4321);
    assertThat(actualRecordingDuration).isEqualTo(Duration.ofMinutes(1));
    assertThat(actualJfr).isSameAs(jfr);
    assertThat(actualMaxAgeDuration).isEqualTo(Duration.ofMinutes(1).multipliedBy(10));
    assertThat(actualKeepRecordingFiles).isFalse();
    assertThat(actualRecorder).isSameAs(recorder);
    assertThat(recording).isNotNull();

    Map<String, String> settings = fieldValue(recorder, "settings");
    assertThat(settings).containsEntry("jdk.ThreadDump#period", "123 ms");
  }

  @Test
  void requestStartOnlyStartsOnce(@TempDir Path tempDir) {
    config.profilerDirectory = tempDir.toString();
    when(jfr.isAvailable()).thenReturn(true);

    supervisor = createSupervisor();

    supervisor.requestStart();
    RecordingSequencer sequencer = awaitSequencer();
    supervisor.requestStart();

    await()
        .during(Duration.ofMillis(200))
        .untilAsserted(() -> assertThat(currentSequencer()).isSameAs(sequencer));
    assertThat(config.logCalled).isTrue();
    verify(jfr).setStackDepth(1024);
  }

  @Test
  void requestStartCreatesConfiguredOutputDirectoryWhenKeepingFiles(@TempDir Path tempDir) {
    Path outputDir = tempDir.resolve("profiling-output");
    config.profilerDirectory = outputDir.toString();
    config.callStackInterval = Duration.ZERO;
    config.keepFiles = true;
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
    config.profilerDirectory = outputPath.toString();
    config.callStackInterval = Duration.ZERO;
    config.keepFiles = true;
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
