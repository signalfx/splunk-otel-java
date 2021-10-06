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

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;
import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsible for starting a single JFR recording. */
class JfrRecorder {
  private static final Logger logger = LoggerFactory.getLogger(JfrRecorder.class.getName());
  static final String RECORDING_NAME = "otel_agent_jfr_profiler";
  private final Map<String, String> settings;

  private final Duration maxAgeDuration;
  private final JFR jfr;
  private final Consumer<Path> onNewRecordingFile;
  private final RecordingFileNamingConvention namingConvention;
  private volatile Recording recording;
  private volatile Instant snapshotStart = Instant.now();

  JfrRecorder(Builder builder) {
    this.settings = requireNonNull(builder.settings);
    this.maxAgeDuration = requireNonNull(builder.maxAgeDuration);
    this.jfr = requireNonNull(builder.jfr);
    this.onNewRecordingFile = requireNonNull(builder.onNewRecordingFile);
    this.namingConvention = requireNonNull(builder.namingConvention);
  }

  public void start() {
    logger.debug("Profiler is starting a JFR recording");
    recording = newRecording();
    recording.setSettings(settings);
    recording.setToDisk(false);
    recording.setName(RECORDING_NAME);
    recording.setDuration(null); // record forever
    recording.setMaxAge(maxAgeDuration);
    recording.start();
  }

  @VisibleForTesting
  Recording newRecording() {
    return new Recording();
  }

  public void flushSnapshot() {
    try (Recording snap = jfr.takeSnapshot()) {
      Path path = namingConvention.newOutputPath();
      logger.debug("Flushing a JFR snapshot: {}", path);
      Instant snapshotEnd = snap.getStopTime();
      try (InputStream in = snap.getStream(snapshotStart, snapshotEnd)) {
        Files.copy(in, path);
        logger.debug("Wrote JFR dump {} with size {}", path, path.toFile().length());
      }
      snapshotStart = snapshotEnd;
      onNewRecordingFile.accept(path);
    } catch (IOException e) {
      logger.error("Error flushing JFR snapshot data to disk", e);
    }
  }

  public boolean isStarted() {
    return (recording != null) && RecordingState.RUNNING.equals(recording.getState());
  }

  public void stop() {
    recording.stop();
    recording = null;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private RecordingFileNamingConvention namingConvention;
    private Map<String, String> settings;
    private Duration maxAgeDuration;
    private JFR jfr = JFR.instance;
    public Consumer<Path> onNewRecordingFile;

    public Builder settings(Map<String, String> settings) {
      this.settings = settings;
      return this;
    }

    public Builder maxAgeDuration(Duration recordingDuration) {
      this.maxAgeDuration = recordingDuration;
      return this;
    }

    public Builder jfr(JFR jfr) {
      this.jfr = jfr;
      return this;
    }

    public Builder onNewRecordingFile(Consumer<Path> onNewRecordingFile) {
      this.onNewRecordingFile = onNewRecordingFile;
      return this;
    }

    public Builder namingConvention(RecordingFileNamingConvention namingConvention) {
      this.namingConvention = namingConvention;
      return this;
    }

    public JfrRecorder build() {
      return new JfrRecorder(this);
    }
  }
}
