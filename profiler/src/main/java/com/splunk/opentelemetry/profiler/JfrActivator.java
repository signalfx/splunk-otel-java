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
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_KEEP_FILES;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_PROFILER_DIRECTORY;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_RECORDING_DURATION_SECONDS;
import static com.splunk.opentelemetry.profiler.JfrFileLifecycleEvents.buildOnFileFinished;
import static com.splunk.opentelemetry.profiler.JfrFileLifecycleEvents.buildOnNewRecording;
import static com.splunk.opentelemetry.profiler.util.HelpfulExecutors.logUncaught;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.logs.BatchingLogsProcessor;
import com.splunk.opentelemetry.logs.LogEntry;
import com.splunk.opentelemetry.logs.LogsExporter;
import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.util.FileDeleter;
import com.splunk.opentelemetry.profiler.util.HelpfulExecutors;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.spi.ComponentInstaller;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(ComponentInstaller.class)
public class JfrActivator implements ComponentInstaller {

  private static final Logger logger = LoggerFactory.getLogger(JfrActivator.class);
  private static final int MAX_BATCH_SIZE = 250;
  private static final Duration MAX_TIME_BETWEEN_BATCHES = Duration.ofSeconds(10);
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

    Path outputDir = Paths.get(config.getProperty(CONFIG_KEY_PROFILER_DIRECTORY, "."));
    RecordingFileNamingConvention namingConvention = new RecordingFileNamingConvention(outputDir);

    RecordingEscapeHatch recordingEscapeHatch =
        RecordingEscapeHatch.builder()
            .namingConvention(namingConvention)
            .configKeepsFilesOnDisk(keepFiles(config))
            .recordingDuration(recordingDuration)
            .build();
    JfrSettingsReader settingsReader = new JfrSettingsReader();

    RecordedEventStream.Factory recordedEventStreamFactory =
        () -> new FilterSortedRecordingFile(() -> new BasicJfrRecordingFile(JFR.instance));

    SpanContextualizer spanContextualizer = new SpanContextualizer();
    LogEntryCreator logEntryCreator = new LogEntryCreator();
    LogsExporter logsExporter = buildExporter();
    Consumer<List<LogEntry>> exportAction = logsExporter::export;
    ScheduledExecutorService exportExecutorService =
        HelpfulExecutors.newSingleThreadedScheduledExecutor("Batched Logs Exporter");
    BatchingLogsProcessor batchingLogsProcessor =
        BatchingLogsProcessor.builder()
            .maxTimeBetweenBatches(MAX_TIME_BETWEEN_BATCHES)
            .maxBatchSize(MAX_BATCH_SIZE)
            .batchAction(exportAction)
            .executorService(exportExecutorService)
            .build();
    batchingLogsProcessor.start();
    StackToSpanLinkageProcessor processor =
        new StackToSpanLinkageProcessor(logEntryCreator, batchingLogsProcessor);

    ThreadDumpProcessor threadDumpProcessor =
        new ThreadDumpProcessor(spanContextualizer, processor);
    EventProcessingChain eventProcessingChain =
        new EventProcessingChain(spanContextualizer, threadDumpProcessor);
    Consumer<Path> deleter = buildFileDeleter(config);
    JfrDirCleanup dirCleanup = new JfrDirCleanup(deleter);

    Consumer<Path> onFileFinished = buildOnFileFinished(deleter, dirCleanup);

    JfrPathHandler jfrPathHandler =
        JfrPathHandler.builder()
            .recordedEventStreamFactory(recordedEventStreamFactory)
            .eventProcessingChain(eventProcessingChain)
            .onFileFinished(onFileFinished)
            .build();

    Consumer<Path> onNewRecording = buildOnNewRecording(jfrPathHandler, dirCleanup);

    JfrRecorder recorder =
        JfrRecorder.builder()
            .settingsReader(settingsReader)
            .maxAgeDuration(recordingDuration.multipliedBy(10))
            .jfr(JFR.instance)
            .namingConvention(namingConvention)
            .onNewRecordingFile(onNewRecording)
            .build();

    RecordingSequencer sequencer =
        RecordingSequencer.builder()
            .recordingDuration(recordingDuration)
            .recordingEscapeHatch(recordingEscapeHatch)
            .recorder(recorder)
            .build();

    sequencer.start();
    dirCleanup.registerShutdownHook();
  }

  private LogsExporter buildExporter() {
    return new LogsExporter() {
      @Override
      public void export(List<LogEntry> logs) {
        // stubbed for now!
        logger.debug("Not exporting {} logs (stubbed)", logs.size());
      }
    };
  }

  private Consumer<Path> buildFileDeleter(Config config) {
    if (keepFiles(config)) {
      logger.warn("{} is enabled, leaving JFR files on disk.", CONFIG_KEY_KEEP_FILES);
      return FileDeleter.noopFileDeleter();
    }
    return FileDeleter.newDeleter();
  }

  private boolean keepFiles(Config config) {
    return config.getBooleanProperty(CONFIG_KEY_KEEP_FILES, false);
  }
}
