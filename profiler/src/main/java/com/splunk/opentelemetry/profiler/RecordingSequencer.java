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

import com.splunk.opentelemetry.profiler.util.HelpfulExecutors;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for periodically generating a sequence of JFR recording files. Prior to starting a
 * recording, it consults with a RecordingStartPredicate to make sure that it is safe/relevant to
 * do.
 */
public class RecordingSequencer {
  private static final Logger logger = LoggerFactory.getLogger(RecordingSequencer.class.getName());
  // The overlap factor causes recordings to be shorter than the requested duration.
  // This forces overlap between recordings and ensures data is continuous when deduplicated.
  public static final double OVERLAP_FACTOR = 0.8;

  private final ScheduledExecutorService executor =
      HelpfulExecutors.newSingleThreadedScheduledExecutor("JFR Recording Sequencer");
  private final Duration recordingDuration;
  private final RecordingStartPredicate recordingStartPredicate;
  private final JfrRecorder recorder;

  public RecordingSequencer(Builder builder) {
    this.recordingDuration = builder.recordingDuration;
    this.recordingStartPredicate = builder.recordingStartPredicate;
    this.recorder = builder.recorder;
  }

  public void start() {
    int period = (int) (recordingDuration.toMillis() * OVERLAP_FACTOR);
    recorder.start();
    executor.scheduleAtFixedRate(this::createRecording, period, period, TimeUnit.MILLISECONDS);
  }

  private void createRecording() {
    if (recordingStartPredicate.canStart()) {
      recorder.flushSnapshot();
      return;
    }
    logger.warn("JFR recordings cannot start.");
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Duration recordingDuration;
    private RecordingStartPredicate recordingStartPredicate;
    private JfrRecorder recorder;

    public Builder recordingDuration(Duration duration) {
      this.recordingDuration = duration;
      return this;
    }

    public Builder recordingStartPredicate(RecordingStartPredicate prediate) {
      this.recordingStartPredicate = prediate;
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
