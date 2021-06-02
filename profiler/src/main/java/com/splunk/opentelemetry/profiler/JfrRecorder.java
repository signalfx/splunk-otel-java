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
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsible for starting a single JFR recording. */
public class JfrRecorder {
  private static final Logger logger = LoggerFactory.getLogger(JfrRecorder.class.getName());
  static final String RECORDING_NAME = "otel_agent_jfr_profiler";
  private final JfrSettingsReader settingsReader;
  private final Duration maxAgeDuration;
  private final JFR jfr;
  private volatile Recording recording;

  JfrRecorder(Builder builder) {
    this.settingsReader = requireNonNull(builder.settingsReader);
    this.maxAgeDuration = requireNonNull(builder.maxAgeDuration);
    this.jfr = requireNonNull(builder.jfr);
  }

  public void start() {
    logger.debug("Profiler is starting a JFR recording");
    recording = newRecording();
    Map<String, String> settings = settingsReader.read();
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
      Path path = Path.of(Instant.now().toString() + ".jfr");
      logger.debug("Flushing a JFR snapshot: {}", path);
      snap.dump(path);
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
    private JfrSettingsReader settingsReader;
    private Duration maxAgeDuration;
    private JFR jfr = JFR.instance;

    public Builder settingsReader(JfrSettingsReader settingsReader) {
      this.settingsReader = settingsReader;
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

    public JfrRecorder build() {
      return new JfrRecorder(this);
    }
  }
}
