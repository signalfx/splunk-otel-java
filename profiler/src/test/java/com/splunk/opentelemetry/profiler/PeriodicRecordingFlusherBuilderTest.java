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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.resources.Resource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;

class PeriodicRecordingFlusherBuilderTest {

  @TempDir Path tempDir;

  @Test
  void buildConfiguresJfrAndWiresRecorderIntoSequencer() {
    JFR jfr = mock(JFR.class);
    TestProfilingConfig config = config(tempDir);
    config.stackDepth = 73;
    config.recordingDuration = Duration.ofMillis(100);

    try (MockedConstruction<JfrRecorder> recorderConstruction =
        mockConstruction(JfrRecorder.class)) {
      PeriodicRecordingFlusher sequencer =
          PeriodicRecordingFlusher.builder(config, Resource.empty()).jfr(jfr).build();

      assertThat(sequencer).isNotNull();
      assertThat(recorderConstruction.constructed()).hasSize(1);
      verify(jfr).setStackDepth(73);

      JfrRecorder recorder = recorderConstruction.constructed().get(0);
      when(recorder.isStarted()).thenReturn(true);

      sequencer.handleInterval();

      verify(recorder).flushSnapshot();
    }
  }

  @Test
  void buildCreatesMissingOutputDirectoryWhenKeepingFiles() {
    Path outputDir = tempDir.resolve("profiler-output");
    JFR jfr = mock(JFR.class);
    TestProfilingConfig config = config(outputDir);
    config.keepFiles = true;

    try (MockedConstruction<JfrRecorder> recorderConstruction =
        mockConstruction(JfrRecorder.class)) {
      PeriodicRecordingFlusher sequencer =
          PeriodicRecordingFlusher.builder(config, Resource.empty()).jfr(jfr).build();

      assertThat(sequencer).isNotNull();
      assertThat(outputDir).isDirectory();
      assertThat(recorderConstruction.constructed()).hasSize(1);
    }
  }

  @Test
  void buildContinuesWhenKeepFilesPathIsNotADirectory() throws Exception {
    Path outputFile = tempDir.resolve("profiler-output");
    Files.createFile(outputFile);
    JFR jfr = mock(JFR.class);
    TestProfilingConfig config = config(outputFile);
    config.keepFiles = true;

    try (MockedConstruction<JfrRecorder> recorderConstruction =
        mockConstruction(JfrRecorder.class)) {
      PeriodicRecordingFlusher sequencer =
          PeriodicRecordingFlusher.builder(config, Resource.empty()).jfr(jfr).build();

      assertThat(sequencer).isNotNull();
      assertThat(recorderConstruction.constructed()).hasSize(1);
    }
  }

  @Test
  void buildRejectsUnsupportedConfigProperties() {
    JFR jfr = mock(JFR.class);
    TestProfilingConfig config = config(tempDir);
    config.configProperties = new Object();

    assertThatThrownBy(
            () ->  PeriodicRecordingFlusher.builder(config, Resource.empty()).jfr(jfr).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("Unsupported config properties type:");
  }

  private TestProfilingConfig config(Path outputDir) {
    TestProfilingConfig config = new TestProfilingConfig();
    config.profilerDirectory = outputDir.toString();
    return config;
  }
}
