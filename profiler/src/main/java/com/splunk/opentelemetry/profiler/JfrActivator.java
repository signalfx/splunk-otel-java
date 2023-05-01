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
import static java.util.logging.Level.WARNING;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.profiler.Configuration.DataFormat;
import com.splunk.opentelemetry.profiler.allocation.exporter.AllocationEventExporter;
import com.splunk.opentelemetry.profiler.allocation.exporter.PprofAllocationEventExporter;
import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.events.EventPeriods;
import com.splunk.opentelemetry.profiler.exporter.CpuEventExporter;
import com.splunk.opentelemetry.profiler.exporter.PprofCpuEventExporter;
import com.splunk.opentelemetry.profiler.util.FileDeleter;
import com.splunk.opentelemetry.profiler.util.HelpfulExecutors;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

@AutoService(AgentListener.class)
public class JfrActivator implements AgentListener {

  private static final java.util.logging.Logger logger =
      java.util.logging.Logger.getLogger(JfrActivator.class.getName());
  private static final int MAX_BATCH_SIZE = 250;
  private static final Duration MAX_TIME_BETWEEN_BATCHES = Duration.ofSeconds(10);
  private final ExecutorService executor = HelpfulExecutors.newSingleThreadExecutor("JFR Profiler");
  private final ConfigurationLogger configurationLogger = new ConfigurationLogger();

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    ConfigProperties config = autoConfiguredOpenTelemetrySdk.getConfig();
    if (notClearForTakeoff(config)) {
      return;
    }

    configurationLogger.log(config);
    logger.info("JFR profiler is active.");
    executor.submit(
        logUncaught(
            () -> activateJfrAndRunForever(config, autoConfiguredOpenTelemetrySdk.getResource())));
  }

  private boolean notClearForTakeoff(ConfigProperties config) {
    if (!config.getBoolean(CONFIG_KEY_ENABLE_PROFILER, false)) {
      logger.fine("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
      logger.fine("xxxxxxxxx  JFR PROFILER DISABLED!  xxxxxxxxx");
      logger.fine("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
      return true;
    }
    if (!JFR.instance.isAvailable()) {
      logger.warning(
          "JDK Flight Recorder (JFR) is not available in this JVM. Profiling is disabled.");
      return true;
    }

    Path outputDir = Paths.get(config.getString(CONFIG_KEY_PROFILER_DIRECTORY));
    if (!Files.exists(outputDir)) {
      // Try creating the directory for the user...
      try {
        Files.createDirectories(outputDir);
      } catch (IOException e) {
        return outdirWarn(outputDir, "does not exist and could not be created");
      }
    }
    if (!Files.isDirectory(outputDir)) {
      return outdirWarn(outputDir, "exists but is not a directory");
    }

    if (!Files.isWritable(outputDir)) {
      return outdirWarn(outputDir, "exists but is not writable.");
    }
    return false;
  }

  private boolean outdirWarn(Path dir, String suffix) {
    logger.log(
        WARNING,
        "PROFILER WILL NOT BE STARTED: The configured output directory {0} {1}.",
        new Object[] {dir, suffix});
    return true;
  }

  private void activateJfrAndRunForever(ConfigProperties config, Resource resource) {
    Path outputDir = Paths.get(config.getString(CONFIG_KEY_PROFILER_DIRECTORY));
    RecordingFileNamingConvention namingConvention = new RecordingFileNamingConvention(outputDir);

    int stackDepth = Configuration.getStackDepth(config);
    JFR.instance.setStackDepth(stackDepth);

    // can't be null, default value is set in Configuration.getProperties
    Duration recordingDuration = config.getDuration(CONFIG_KEY_RECORDING_DURATION, null);
    RecordingEscapeHatch recordingEscapeHatch =
        RecordingEscapeHatch.builder()
            .namingConvention(namingConvention)
            .configKeepsFilesOnDisk(keepFiles(config))
            .recordingDuration(recordingDuration)
            .build();
    Map<String, String> jfrSettings = buildJfrSettings(config);

    EventReader eventReader = new EventReader();
    SpanContextualizer spanContextualizer = new SpanContextualizer(eventReader);
    EventPeriods periods = new EventPeriods(jfrSettings::get);
    LogRecordExporter logsExporter = LogExporterBuilder.fromConfig(config);

    DataFormat cpuDataFormat = Configuration.getCpuDataFormat(config);
    CpuEventExporter cpuEventExporter =
        PprofCpuEventExporter.builder()
            .otelLogger(buildOtelLogger(SimpleLogRecordProcessor.create(logsExporter), resource))
            .dataFormat(cpuDataFormat)
            .eventPeriods(periods)
            .stackDepth(stackDepth)
            .build();

    StackTraceFilter stackTraceFilter = buildStackTraceFilter(config, eventReader);
    ThreadDumpProcessor threadDumpProcessor =
        buildThreadDumpProcessor(
            eventReader, spanContextualizer, cpuEventExporter, stackTraceFilter, config);

    DataFormat allocationDataFormat = Configuration.getAllocationDataFormat(config);
    AllocationEventExporter allocationEventExporter =
        PprofAllocationEventExporter.builder()
            .eventReader(eventReader)
            .otelLogger(buildOtelLogger(SimpleLogRecordProcessor.create(logsExporter), resource))
            .dataFormat(allocationDataFormat)
            .stackDepth(stackDepth)
            .build();

    TLABProcessor tlabProcessor =
        TLABProcessor.builder(config)
            .eventReader(eventReader)
            .allocationEventExporter(allocationEventExporter)
            .spanContextualizer(spanContextualizer)
            .stackTraceFilter(stackTraceFilter)
            .build();

    EventProcessingChain eventProcessingChain =
        new EventProcessingChain(
            eventReader, spanContextualizer, threadDumpProcessor, tlabProcessor);
    Consumer<Path> deleter = buildFileDeleter(config);
    JfrDirCleanup dirCleanup = new JfrDirCleanup(deleter);

    Consumer<Path> onFileFinished = buildOnFileFinished(deleter, dirCleanup);

    JfrPathHandler jfrPathHandler =
        JfrPathHandler.builder()
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

  private Logger buildOtelLogger(LogRecordProcessor logProcessor, Resource resource) {
    return SdkLoggerProvider.builder()
        .addLogRecordProcessor(logProcessor)
        .setResource(resource)
        .build()
        .loggerBuilder(ProfilingSemanticAttributes.OTEL_INSTRUMENTATION_NAME)
        .setInstrumentationVersion(ProfilingSemanticAttributes.OTEL_INSTRUMENTATION_VERSION)
        .build();
  }

  private ThreadDumpProcessor buildThreadDumpProcessor(
      EventReader eventReader,
      SpanContextualizer spanContextualizer,
      CpuEventExporter profilingEventExporter,
      StackTraceFilter stackTraceFilter,
      ConfigProperties config) {
    return ThreadDumpProcessor.builder()
        .eventReader(eventReader)
        .spanContextualizer(spanContextualizer)
        .cpuEventExporter(profilingEventExporter)
        .stackTraceFilter(stackTraceFilter)
        .onlyTracingSpans(Configuration.getTracingStacksOnly(config))
        .build();
  }

  /** Based on config, filters out agent internal stacks and/or JVM internal stacks */
  private StackTraceFilter buildStackTraceFilter(ConfigProperties config, EventReader eventReader) {
    boolean includeAgentInternalStacks = Configuration.getIncludeAgentInternalStacks(config);
    boolean includeJVMInternalStacks = Configuration.getIncludeJvmInternalStacks(config);
    return new StackTraceFilter(eventReader, includeAgentInternalStacks, includeJVMInternalStacks);
  }

  private Map<String, String> buildJfrSettings(ConfigProperties config) {
    JfrSettingsReader settingsReader = new JfrSettingsReader();
    Map<String, String> jfrSettings = settingsReader.read();
    JfrSettingsOverrides overrides = new JfrSettingsOverrides(config);
    return overrides.apply(jfrSettings);
  }

  private Consumer<Path> buildFileDeleter(ConfigProperties config) {
    if (keepFiles(config)) {
      logger.log(WARNING, "{0} is enabled, leaving JFR files on disk.", CONFIG_KEY_KEEP_FILES);
      return FileDeleter.noopFileDeleter();
    }
    return FileDeleter.newDeleter();
  }

  private boolean keepFiles(ConfigProperties config) {
    return config.getBoolean(CONFIG_KEY_KEEP_FILES, false);
  }

  private static class BatchLogProcessorHolder {
    private static LogRecordProcessor INSTANCE;

    // initialize BatchLogProcessor only if it is needed and if it is needed initialize it only
    // once
    static LogRecordProcessor get(LogRecordExporter logsExporter) {

      if (INSTANCE == null) {
        INSTANCE =
            BatchLogRecordProcessor.builder(logsExporter)
                .setScheduleDelay(MAX_TIME_BETWEEN_BATCHES)
                .setMaxExportBatchSize(MAX_BATCH_SIZE)
                .build();
        // BatchLogProcessor auto-starts in constructor. No need to invoke start()
      }

      return INSTANCE;
    }
  }
}
