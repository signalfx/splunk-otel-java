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
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;
import jdk.jfr.Recording;
import jdk.jfr.RecordingState;

/** Responsible for starting a single JFR recording. */
class JfrRecorder {
  private static final Logger logger = Logger.getLogger(JfrRecorder.class.getName());

  private static final int BUFFER_SIZE = 8192;
  static final String RECORDING_NAME = "otel_agent_jfr_profiler";
  private final Map<String, String> settings;

  private final Duration maxAgeDuration;
  private final JFR jfr;
  private final Consumer<InputStream> onNewRecording;
  private final RecordingFileNamingConvention namingConvention;
  private final boolean keepRecordingFiles;
  private volatile Recording recording;
  private volatile Instant snapshotStart = Instant.now();

  JfrRecorder(Builder builder) {
    this.settings = requireNonNull(builder.settings);
    this.maxAgeDuration = requireNonNull(builder.maxAgeDuration);
    this.jfr = requireNonNull(builder.jfr);
    this.onNewRecording = requireNonNull(builder.onNewRecording);
    this.namingConvention = requireNonNull(builder.namingConvention);
    this.keepRecordingFiles = builder.keepRecordingFiles;
  }

  public void start() {
    logger.fine("Profiler is starting a JFR recording");
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
      Instant snapshotEnd = snap.getStopTime();
      Instant start = snapshotStart;
      snapshotStart = snapshotEnd;
      if (keepRecordingFiles) {
        Path path = namingConvention.newOutputPath().toAbsolutePath();
        logger.log(FINE, "Flushing a JFR snapshot: {0}", path);
        try (InputStream in = snap.getStream(start, snapshotEnd)) {
          try (OutputStream out = Files.newOutputStream(path)) {
            copy(in, out);
            if (logger.isLoggable(FINE)) {
              logger.log(
                  FINE,
                  "Wrote JFR dump {0} with size {1}",
                  new Object[] {path, path.toFile().length()});
            }
          }
        }
        try (InputStream in = Files.newInputStream(path)) {
          onNewRecording.accept(in);
        }
      } else {
        try (InputStream in = snap.getStream(start, snapshotEnd)) {
          onNewRecording.accept(in);
        }
      }
    } catch (IOException e) {
      logger.log(SEVERE, "Error handling JFR recording", e);
    }
  }

  private static void copy(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    int read;
    while ((read = in.read(buffer, 0, BUFFER_SIZE)) >= 0) {
      out.write(buffer, 0, read);
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
    private JFR jfr = JFR.getInstance();
    private Consumer<InputStream> onNewRecording;
    private boolean keepRecordingFiles;

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

    public Builder onNewRecording(Consumer<InputStream> onNewRecording) {
      this.onNewRecording = onNewRecording;
      return this;
    }

    public Builder namingConvention(RecordingFileNamingConvention namingConvention) {
      this.namingConvention = namingConvention;
      return this;
    }

    public Builder keepRecordingFiles(boolean keepRecordingFiles) {
      this.keepRecordingFiles = keepRecordingFiles;
      return this;
    }

    public JfrRecorder build() {
      return new JfrRecorder(this);
    }
  }
}
