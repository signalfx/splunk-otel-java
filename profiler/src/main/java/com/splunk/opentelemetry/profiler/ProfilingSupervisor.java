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

import static io.opentelemetry.api.incubator.config.DeclarativeConfigProperties.empty;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getResource;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

import com.google.common.annotations.VisibleForTesting;
import com.splunk.opentelemetry.profiler.allocation.exporter.AllocationEventExporter;
import com.splunk.opentelemetry.profiler.allocation.exporter.PprofAllocationEventExporter;
import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.exporter.CpuEventExporter;
import com.splunk.opentelemetry.profiler.exporter.PprofCpuEventExporter;
import com.splunk.opentelemetry.profiler.util.HelpfulExecutors;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class oversees the profiling subsystem. It runs for the entire time that the agent is
 * running.
 */
public class ProfilingSupervisor {

  private static final java.util.logging.Logger logger =
      java.util.logging.Logger.getLogger(ProfilingSupervisor.class.getName());
  private final ProfilerConfiguration config;
  private final JFR jfr;
  private final AutoConfiguredOpenTelemetrySdk sdk;
  private final BlockingQueue<ProfilingCommand> commandQueue;
  private final AtomicReference<RecordingSequencer> sequencer = new AtomicReference<>();

  private ProfilingSupervisor(
      ProfilerConfiguration config,
      JFR jfr,
      AutoConfiguredOpenTelemetrySdk sdk,
      BlockingQueue<ProfilingCommand> commandQueue) {
    this.config = config;
    this.jfr = jfr;
    this.sdk = sdk;
    this.commandQueue = commandQueue;
  }

  static ProfilingSupervisor createAndStart(
      AutoConfiguredOpenTelemetrySdk sdk, ProfilerConfiguration config) {
    return createAndStart(
        config, JFR.getInstance(), HelpfulExecutors.newSingleThreadExecutor("JFR Profiler"), sdk);
  }

  @VisibleForTesting
  static ProfilingSupervisor createAndStart(
      ProfilerConfiguration config,
      JFR jfr,
      ExecutorService executor,
      AutoConfiguredOpenTelemetrySdk sdk) {
    // TODO: What if already started?
    BlockingQueue<ProfilingCommand> queue = new LinkedBlockingQueue<>();
    ProfilingSupervisor supervisor = new ProfilingSupervisor(config, jfr, sdk, queue);
    executor.submit(supervisor::forever);
    return supervisor;
  }

  private void forever() {
    while (true) {
      try {
        ProfilingCommand command = commandQueue.take();
        handleCommand(command);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        logger.fine("ProfilingSupervisor is shutting down");
        return;
      } catch (Exception e) {
        logger.log(FINE, "ProfilingSupervisor encountered an unexpected exception", e);
      }
    }
  }

  public void requestStart() {
    commandQueue.add(ProfilingCommand.START);
  }

  public void requestStop() {
    commandQueue.add(ProfilingCommand.STOP);
  }

  private void handleCommand(ProfilingCommand command) {
    switch (command) {
      case START:
        tryStart();
        break;
      case STOP:
        // TODO: Build me
        logger.warning("ProfilingSupervisor STOP not yet implemented");
        break;
    }
  }

  /**
   * Try and start the profiler. This does not check configuration, just responds to a command
   * request.
   */
  private void tryStart() {
    if (alreadyRunning()) {
      logger.warning("JFR is already running, not starting again.");
      return;
    }
    if (!jfr.isAvailable()) {
      logger.warning(
          "JDK Flight Recorder (JFR) is not available in this JVM. Profiling will not start.");
      return;
    }
    config.log();
    logger.info("Profiler is active.");
    setupContextStorage();
    activateJfrAndRunUntilStopped(config, getResource(sdk));
  }

  private boolean alreadyRunning() {
    return sequencer.get() != null;
  }

  private boolean checkOutputDir(Path outputDir) {
    if (!Files.exists(outputDir)) {
      // Try creating the directory for the user...
      try {
        Files.createDirectories(outputDir);
      } catch (IOException e) {
        outdirWarn(outputDir, "does not exist and could not be created");
        return false;
      }
    }
    if (!Files.isDirectory(outputDir)) {
      outdirWarn(outputDir, "exists but is not a directory");
      return false;
    }

    if (!Files.isWritable(outputDir)) {
      outdirWarn(outputDir, "exists but is not writable");
      return false;
    }

    return true;
  }

  private void outdirWarn(Path dir, String suffix) {
    logger.log(WARNING, "The configured output directory {0} {1}.", new Object[] {dir, suffix});
  }

  private void activateJfrAndRunUntilStopped(ProfilerConfiguration config, Resource resource) {
    boolean keepFiles = config.getKeepFiles();
    Path outputDir = Paths.get(config.getProfilerDirectory());
    if (keepFiles && !checkOutputDir(outputDir)) {
      keepFiles = false;
    }
    RecordingFileNamingConvention namingConvention = new RecordingFileNamingConvention(outputDir);

    int stackDepth = config.getStackDepth();
    jfr.setStackDepth(stackDepth);

    Duration recordingDuration = config.getRecordingDuration();
    Map<String, String> jfrSettings = buildJfrSettings(config);

    EventReader eventReader = new EventReader();
    SpanContextualizer spanContextualizer = new SpanContextualizer(eventReader);
    LogRecordExporter logsExporter = createLogRecordExporter(config.getConfigProperties());

    CpuEventExporter cpuEventExporter =
        PprofCpuEventExporter.builder()
            .otelLogger(buildOtelLogger(SimpleLogRecordProcessor.create(logsExporter), resource))
            .period(config.getCallStackInterval())
            .stackDepth(stackDepth)
            .build();

    StackTraceFilter stackTraceFilter = buildStackTraceFilter(config, eventReader);
    ThreadDumpProcessor threadDumpProcessor =
        buildThreadDumpProcessor(
            eventReader, spanContextualizer, cpuEventExporter, stackTraceFilter, config);

    AllocationEventExporter allocationEventExporter =
        PprofAllocationEventExporter.builder()
            .eventReader(eventReader)
            .otelLogger(buildOtelLogger(SimpleLogRecordProcessor.create(logsExporter), resource))
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

    JfrRecordingHandler jfrRecordingHandler =
        JfrRecordingHandler.builder().eventProcessingChain(eventProcessingChain).build();

    JfrRecorder recorder =
        JfrRecorder.builder()
            .settings(jfrSettings)
            .maxAgeDuration(recordingDuration.multipliedBy(10))
            .jfr(jfr)
            .onNewRecording(jfrRecordingHandler)
            .namingConvention(namingConvention)
            .keepRecordingFiles(keepFiles)
            .build();

    RecordingSequencer recordingSequencer =
        RecordingSequencer.builder()
            .recordingDuration(recordingDuration)
            .recorder(recorder)
            .build();
    if (sequencer.compareAndSet(null, recordingSequencer)) {
      recordingSequencer.start();
    }
  }

  private static LogRecordExporter createLogRecordExporter(Object configProperties) {
    if (configProperties instanceof DeclarativeConfigProperties) {
      DeclarativeConfigProperties exporterConfig =
          ((DeclarativeConfigProperties) configProperties).getStructured("exporter", empty());
      return LogExporterBuilder.fromConfig(exporterConfig);
    }
    if (configProperties instanceof ConfigProperties) {
      return LogExporterBuilder.fromConfig((ConfigProperties) configProperties);
    }
    throw new IllegalArgumentException(
        "Unsupported config properties type: " + configProperties.getClass().getName());
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
      ProfilerConfiguration config) {
    return ThreadDumpProcessor.builder()
        .eventReader(eventReader)
        .spanContextualizer(spanContextualizer)
        .cpuEventExporter(profilingEventExporter)
        .stackTraceFilter(stackTraceFilter)
        .onlyTracingSpans(config.getTracingStacksOnly())
        .build();
  }

  /** Based on config, filters out agent internal stacks and/or JVM internal stacks */
  private StackTraceFilter buildStackTraceFilter(
      ProfilerConfiguration config, EventReader eventReader) {
    boolean includeAgentInternalStacks = config.getIncludeAgentInternalStacks();
    boolean includeJVMInternalStacks = config.getIncludeJvmInternalStacks();
    return new StackTraceFilter(eventReader, includeAgentInternalStacks, includeJVMInternalStacks);
  }

  private Map<String, String> buildJfrSettings(ProfilerConfiguration config) {
    JfrSettingsReader settingsReader = new JfrSettingsReader();
    Map<String, String> jfrSettings = settingsReader.read();
    JfrSettingsOverrides overrides = new JfrSettingsOverrides(config);
    return overrides.apply(jfrSettings);
  }

  private static void setupContextStorage() {
    ContextStorage.addWrapper(JfrContextStorage::new);
  }

  enum ProfilingCommand {
    START,
    STOP
  }
}
