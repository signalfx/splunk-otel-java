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

import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_ENABLE_PROFILER;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_PROFILER_DIRECTORY;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_RECORDING_DURATION_SECONDS;
import static com.splunk.opentelemetry.profiler.util.HelpfulExecutors.logUncaught;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.profiler.util.HelpfulExecutors;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.spi.ComponentInstaller;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(ComponentInstaller.class)
public class JfrActivator implements ComponentInstaller {

  private static final Logger logger = LoggerFactory.getLogger(JfrActivator.class.getName());
  private final ExecutorService executor = HelpfulExecutors.newSingleThreadExecutor("JFR Profiler");

  @Override
  public void afterByteBuddyAgent(Config config) {
    if (!config.getBooleanProperty(CONFIG_KEY_ENABLE_PROFILER, false)) {
      logger.debug("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
      logger.debug("xxxxxxxxx  JFR PROFILER DISABLED!  xxxxxxxxx");
      logger.debug("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
      return;
    }
    if (!JFR.instance.isAvailable()) {
      logger.warn("Java Flight Recorder is not available in this JVM. Profiling is disabled.");
      return;
    }
    logger.info("JFR profiler is active.");
    executor.submit(logUncaught(() -> activateJfrAndRunForever(config)));
  }

  private void activateJfrAndRunForever(Config config) {
    String recordingDurationStr = config.getProperty(CONFIG_KEY_RECORDING_DURATION_SECONDS, "20");
    Duration recordingDuration = Duration.ofSeconds(Integer.parseInt(recordingDurationStr));
    RecordingEscapeHatch recordingEscapeHatch = new RecordingEscapeHatch();
    JfrSettingsReader settingsReader = new JfrSettingsReader();
    Path outputDir = Paths.get(config.getProperty(CONFIG_KEY_PROFILER_DIRECTORY, "."));
    JfrRecorder recorder =
        JfrRecorder.builder()
            .settingsReader(settingsReader)
            .maxAgeDuration(recordingDuration.multipliedBy(10))
            .jfr(JFR.instance)
            .outputDir(outputDir)
            .build();
    RecordingSequencer sequencer =
        RecordingSequencer.builder()
            .recordingDuration(recordingDuration)
            .recordingEscapeHatch(recordingEscapeHatch)
            .recorder(recorder)
            .build();
    sequencer.start();
  }
}
