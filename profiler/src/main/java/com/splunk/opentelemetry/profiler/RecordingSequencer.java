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

import static java.util.logging.Level.SEVERE;

import com.google.common.annotations.VisibleForTesting;
import com.splunk.opentelemetry.profiler.util.HelpfulExecutors;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Responsible for periodically generating a sequence of JFR recordings.
 */
class RecordingSequencer {
  private static final Logger logger = Logger.getLogger(RecordingSequencer.class.getName());

  private final ScheduledExecutorService executor =
      HelpfulExecutors.newSingleThreadedScheduledExecutor("JFR Recording Sequencer");
  private final Duration recordingDuration;
  private final JfrRecorder recorder;

  private RecordingSequencer(Builder builder) {
    this.recordingDuration = builder.recordingDuration;
    this.recorder = builder.recorder;
  }

  public void start() {
    recorder.start();
    executor.scheduleAtFixedRate(
        this::handleInterval, 0, recordingDuration.toMillis(), TimeUnit.MILLISECONDS);
  }

  @VisibleForTesting
  void handleInterval() {
    try {
      if (!recorder.isStarted()) {
        recorder.start();
        return;
      }
      recorder.flushSnapshot();
    } catch (Throwable throwable) {
      logger.log(SEVERE, "Profiler periodic task failed.", throwable);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Duration recordingDuration;
    private JfrRecorder recorder;

    public Builder recordingDuration(Duration duration) {
      this.recordingDuration = duration;
      return this;
    }

    public Builder recorder(JfrRecorder recorder) {
      this.recorder = recorder;
      return this;
    }

    public RecordingSequencer build() {
      return new RecordingSequencer(this);
    }
  }
}
