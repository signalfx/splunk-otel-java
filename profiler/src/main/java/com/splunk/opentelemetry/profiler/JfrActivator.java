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
import static com.splunk.opentelemetry.profiler.util.Runnables.logUncaught;
import static java.util.logging.Level.WARNING;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.profiler.allocation.exporter.AllocationEventExporter;
import com.splunk.opentelemetry.profiler.allocation.exporter.PprofAllocationEventExporter;
import com.splunk.opentelemetry.profiler.context.SpanContextualizer;
import com.splunk.opentelemetry.profiler.contextstorage.JavaContextStorage;
import com.splunk.opentelemetry.profiler.exporter.CpuEventExporter;
import com.splunk.opentelemetry.profiler.exporter.PprofCpuEventExporter;
import com.splunk.opentelemetry.profiler.util.HelpfulExecutors;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.javaagent.extension.AgentListener;
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
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@AutoService(AgentListener.class)
public class JfrActivator implements AgentListener {

  private static final java.util.logging.Logger logger =
      java.util.logging.Logger.getLogger(JfrActivator.class.getName());
  private final ConfigurationLogger configurationLogger = new ConfigurationLogger();

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    ConfigProperties config = autoConfiguredOpenTelemetrySdk.getConfig();
    if (!config.getBoolean(CONFIG_KEY_ENABLE_PROFILER, false)) {
      logger.fine("Profiler is not enabled.");
      return;
    }
    boolean useJfr = Configuration.getProfilerJfrEnabled(config);
    if (useJfr && !JFR.instance.isAvailable()) {
      logger.fine(
          "JDK Flight Recorder (JFR) is not available in this JVM, switching to java profiler.");
      if (Configuration.getTLABEnabled(config)) {
        logger.warning(
            "JDK Flight Recorder (JFR) is not available in this JVM. Memory profiling is disabled.");
      }
      useJfr = false;
    }

    configurationLogger.log(config);
    logger.info("Profiler is active.");

    if (useJfr) {
      JfrProfiler.run(this, config, autoConfiguredOpenTelemetrySdk.getResource());
    } else {
      JavaProfiler.run(this, config, autoConfiguredOpenTelemetrySdk.getResource());
    }
  }

  private static class JfrProfiler {
    private static final ExecutorService executor =
        HelpfulExecutors.newSingleThreadExecutor("JFR Profiler");

    static void run(JfrActivator activator, ConfigProperties config, Resource resource) {
      executor.submit(logUncaught(() -> activator.activateJfrAndRunForever(config, resource)));
    }
  }

  private static class JavaProfiler {
    private static final ScheduledExecutorService scheduler =
        HelpfulExecutors.newSingleThreadedScheduledExecutor("Profiler scheduler");

    static void run(JfrActivator activator, ConfigProperties config, Resource resource) {
      int stackDepth = Configuration.getStackDepth(config);
      LogRecordExporter logsExporter = LogExporterBuilder.fromConfig(config);
      CpuEventExporter cpuEventExporter =
          PprofCpuEventExporter.builder()
              .otelLogger(
                  activator.buildOtelLogger(
                      SimpleLogRecordProcessor.create(logsExporter), resource))
              .period(Configuration.getCallStackInterval(config))
              .stackDepth(stackDepth)
              .build();

      Runnable profiler =
          () -> {
            Instant now = Instant.now();
            Map<Thread, StackTraceElement[]> stackTracesMap;
            Map<Thread, SpanContext> contextMap = new HashMap<>();
            // disallow context changes while we are taking the thread dump
            JavaContextStorage.block();
            try {
              stackTracesMap = Thread.getAllStackTraces();
              // copy active context for each thread
              for (Thread thread : stackTracesMap.keySet()) {
                SpanContext spanContext = JavaContextStorage.activeContext.get(thread);
                if (spanContext != null) {
                  contextMap.put(thread, spanContext);
                }
              }
            } finally {
              JavaContextStorage.unblock();
            }
            for (Map.Entry<Thread, StackTraceElement[]> entry : stackTracesMap.entrySet()) {
              Thread thread = entry.getKey();
              SpanContext spanContext = contextMap.get(thread);
              cpuEventExporter.export(thread, entry.getValue(), now, spanContext);
            }
            cpuEventExporter.flush();
          };
      long period = Configuration.getCallStackInterval(config).toMillis();
      scheduler.scheduleAtFixedRate(
          logUncaught(() -> profiler.run()), period, period, TimeUnit.MILLISECONDS);
    }
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

  private static void outdirWarn(Path dir, String suffix) {
    logger.log(WARNING, "The configured output directory {0} {1}.", new Object[] {dir, suffix});
  }

  private void activateJfrAndRunForever(ConfigProperties config, Resource resource) {
    boolean keepFiles = keepFiles(config);
    Path outputDir = Paths.get(config.getString(CONFIG_KEY_PROFILER_DIRECTORY));
    if (keepFiles && !checkOutputDir(outputDir)) {
      keepFiles = false;
    }
    RecordingFileNamingConvention namingConvention = new RecordingFileNamingConvention(outputDir);

    int stackDepth = Configuration.getStackDepth(config);
    JFR.instance.setStackDepth(stackDepth);

    // can't be null, default value is set in Configuration.getProperties
    Duration recordingDuration = config.getDuration(CONFIG_KEY_RECORDING_DURATION, null);
    Map<String, String> jfrSettings = buildJfrSettings(config);

    EventReader eventReader = new EventReader();
    SpanContextualizer spanContextualizer = new SpanContextualizer(eventReader);
    LogRecordExporter logsExporter = LogExporterBuilder.fromConfig(config);

    CpuEventExporter cpuEventExporter =
        PprofCpuEventExporter.builder()
            .otelLogger(buildOtelLogger(SimpleLogRecordProcessor.create(logsExporter), resource))
            .period(Configuration.getCallStackInterval(config))
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
            .jfr(JFR.instance)
            .onNewRecording(jfrRecordingHandler)
            .namingConvention(namingConvention)
            .keepRecordingFiles(keepFiles)
            .build();

    RecordingSequencer sequencer =
        RecordingSequencer.builder()
            .recordingDuration(recordingDuration)
            .recorder(recorder)
            .build();

    sequencer.start();
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

  private boolean keepFiles(ConfigProperties config) {
    return config.getBoolean(CONFIG_KEY_KEEP_FILES, false);
  }
}
