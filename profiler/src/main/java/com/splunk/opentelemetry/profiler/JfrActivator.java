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
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_RECORDING_DURATION;
import static com.splunk.opentelemetry.profiler.JfrFileLifecycleEvents.buildOnFileFinished;
import static com.splunk.opentelemetry.profiler.JfrFileLifecycleEvents.buildOnNewRecording;
import static com.splunk.opentelemetry.profiler.util.Runnables.logUncaught;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.logs.BatchingLogsProcessor;
import com.splunk.opentelemetry.profiler.Configuration.AllocationDataFormat;
import com.splunk.opentelemetry.profiler.allocation.exporter.AllocationEventExporter;
import com.splunk.opentelemetry.profiler.allocation.exporter.PlainTextAllocationEventExporter;
import com.splunk.opentelemetry.profiler.allocation.exporter.PprofAllocationEventExporter;
import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.events.EventPeriods;
import com.splunk.opentelemetry.profiler.util.FileDeleter;
import com.splunk.opentelemetry.profiler.util.HelpfulExecutors;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.logs.export.LogExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogProcessor;
import io.opentelemetry.sdk.resources.Resource;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(AgentListener.class)
public class JfrActivator implements AgentListener {

  private static final Logger logger = LoggerFactory.getLogger(JfrActivator.class);
  private static final int MAX_BATCH_SIZE = 250;
  private static final Duration MAX_TIME_BETWEEN_BATCHES = Duration.ofSeconds(10);
  private final ExecutorService executor = HelpfulExecutors.newSingleThreadExecutor("JFR Profiler");
  private final ConfigurationLogger configurationLogger = new ConfigurationLogger();

  @Override
  public void afterAgent(
      Config config, AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    if (!config.getBoolean(CONFIG_KEY_ENABLE_PROFILER, false)) {
      logger.debug("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
      logger.debug("xxxxxxxxx  JFR PROFILER DISABLED!  xxxxxxxxx");
      logger.debug("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
      return;
    }
    if (!JFR.instance.isAvailable()) {
      logger.warn("JDK Flight Recorder (JFR) is not available in this JVM. Profiling is disabled.");
      return;
    }
    configurationLogger.log(config);
    logger.info("JFR profiler is active.");
    executor.submit(
        logUncaught(
            () -> activateJfrAndRunForever(config, autoConfiguredOpenTelemetrySdk.getResource())));
  }

  private void activateJfrAndRunForever(Config config, Resource resource) {
    // can't be null, default value is set in Configuration.getProperties
    Duration recordingDuration = config.getDuration(CONFIG_KEY_RECORDING_DURATION, null);

    Path outputDir = Paths.get(config.getString(CONFIG_KEY_PROFILER_DIRECTORY));
    RecordingFileNamingConvention namingConvention = new RecordingFileNamingConvention(outputDir);

    RecordingEscapeHatch recordingEscapeHatch =
        RecordingEscapeHatch.builder()
            .namingConvention(namingConvention)
            .configKeepsFilesOnDisk(keepFiles(config))
            .recordingDuration(recordingDuration)
            .build();
    Map<String, String> jfrSettings = buildJfrSettings(config);

    RecordedEventStream.Factory recordedEventStreamFactory =
        () -> new BasicJfrRecordingFile(JFR.instance);

    SpanContextualizer spanContextualizer = new SpanContextualizer();
    EventPeriods periods = new EventPeriods(jfrSettings::get);
    LogDataCommonAttributes commonAttributes = new LogDataCommonAttributes(periods);
    LogExporter logsExporter = LogExporterBuilder.fromConfig(config);

    ScheduledExecutorService exportExecutorService =
        HelpfulExecutors.newSingleThreadedScheduledExecutor("Batched Logs Exporter");
    BatchingLogsProcessor batchingLogsProcessor =
        BatchingLogsProcessor.builder()
            .maxTimeBetweenBatches(MAX_TIME_BETWEEN_BATCHES)
            .maxBatchSize(MAX_BATCH_SIZE)
            .batchAction(logsExporter)
            .executorService(exportExecutorService)
            .build();
    batchingLogsProcessor.start();

    LogDataCreator logDataCreator = new LogDataCreator(commonAttributes, resource);

    StackToSpanLinkageProcessor processor =
        new StackToSpanLinkageProcessor(logDataCreator, batchingLogsProcessor);

    ThreadDumpProcessor threadDumpProcessor =
        buildThreadDumpProcessor(
            spanContextualizer, processor, buildStackTraceFilter(config), config);

    AllocationDataFormat allocationDataFormat = Configuration.getAllocationDataFormat(config);
    AllocationEventExporter allocationEventExporter;
    if (allocationDataFormat == AllocationDataFormat.TEXT) {
      allocationEventExporter =
          PlainTextAllocationEventExporter.builder()
              .logProcessor(batchingLogsProcessor)
              .commonAttributes(commonAttributes)
              .resource(resource)
              .build();
    } else {
      allocationEventExporter =
          PprofAllocationEventExporter.builder()
              .logProcessor(SimpleLogProcessor.create(logsExporter))
              .resource(resource)
              .allocationDataFormat(allocationDataFormat)
              .build();
    }

    TLABProcessor tlabProcessor =
        TLABProcessor.builder(config)
            .allocationEventExporter(allocationEventExporter)
            .spanContextualizer(spanContextualizer)
            .build();
    EventProcessingChain eventProcessingChain =
        new EventProcessingChain(spanContextualizer, threadDumpProcessor, tlabProcessor);
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
            .settings(jfrSettings)
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

  private ThreadDumpProcessor buildThreadDumpProcessor(
      SpanContextualizer spanContextualizer,
      StackToSpanLinkageProcessor processor,
      StackTraceFilter stackTraceFilter,
      Config config) {
    return ThreadDumpProcessor.builder()
        .spanContextualizer(spanContextualizer)
        .processor(processor)
        .stackTraceFilter(stackTraceFilter)
        .onlyTracingSpans(Configuration.getTracingStacksOnly(config))
        .build();
  }

  /** Based on config, filters out agent internal stacks and/or JVM internal stacks */
  private StackTraceFilter buildStackTraceFilter(Config config) {
    boolean includeAgentInternalStacks = Configuration.getIncludeAgentInternalStacks(config);
    boolean includeJVMInternalStacks = Configuration.getIncludeJvmInternalStacks(config);
    return new StackTraceFilter(includeAgentInternalStacks, includeJVMInternalStacks);
  }

  private Map<String, String> buildJfrSettings(Config config) {
    JfrSettingsReader settingsReader = new JfrSettingsReader();
    Map<String, String> jfrSettings = settingsReader.read();
    JfrSettingsOverrides overrides = new JfrSettingsOverrides(config);
    return overrides.apply(jfrSettings);
  }

  private Consumer<Path> buildFileDeleter(Config config) {
    if (keepFiles(config)) {
      logger.warn("{} is enabled, leaving JFR files on disk.", CONFIG_KEY_KEEP_FILES);
      return FileDeleter.noopFileDeleter();
    }
    return FileDeleter.newDeleter();
  }

  private boolean keepFiles(Config config) {
    return config.getBoolean(CONFIG_KEY_KEEP_FILES, false);
  }
}
