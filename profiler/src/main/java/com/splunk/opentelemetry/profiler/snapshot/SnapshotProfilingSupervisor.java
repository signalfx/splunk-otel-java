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

package com.splunk.opentelemetry.profiler.snapshot;

import static java.util.logging.Level.WARNING;

import com.google.common.annotations.VisibleForTesting;
import com.splunk.opentelemetry.profiler.OtelLoggerFactory;
import com.splunk.opentelemetry.profiler.util.DeclarativeConfigPropertiesUtil;
import com.splunk.opentelemetry.profiler.util.HelpfulExecutors;
import com.splunk.opentelemetry.profiler.util.OptionalConfigurableSupplier;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class SnapshotProfilingSupervisor {
  public static final OptionalConfigurableSupplier<SnapshotProfilingSupervisor> SUPPLIER =
      new OptionalConfigurableSupplier<>();
  private static final Logger logger =
      Logger.getLogger(SnapshotProfilingSupervisor.class.getName());

  private final OptionalConfigurableSupplier<SnapshotProfilingConfiguration> configurationSupplier;
  private final BlockingQueue<ProfilingCommand> commandQueue;
  private final ConfigurableSupplier<StagingArea> stagingAreaSupplier;
  private final ConfigurableSupplier<StackTraceSampler> stackTraceSamplerSupplier;
  private final ConfigurableSupplier<StackTraceExporter> stackTraceExporterSupplier;
  private final ConfigurableSupplier<SpanTracker> spanTrackerSupplier;
  private final OptionalConfigurableSupplier<TraceThreadChangeDetector>
      traceThreadChangeDetectorSupplier;
  private final OptionalConfigurableSupplier<SnapshotProfilingSpanProcessor>
      profilingSpanProcessorSupplier;
  private final AutoConfiguredOpenTelemetrySdk sdk;
  private final OtelLoggerFactory otelLoggerFactory;
  private volatile boolean running;

  @VisibleForTesting
  SnapshotProfilingSupervisor(
      OptionalConfigurableSupplier<SnapshotProfilingConfiguration> configurationSupplier,
      BlockingQueue<ProfilingCommand> commandQueue,
      ConfigurableSupplier<StagingArea> stagingAreaSupplier,
      ConfigurableSupplier<StackTraceSampler> stackTraceSamplerSupplier,
      ConfigurableSupplier<StackTraceExporter> stackTraceExporterSupplier,
      ConfigurableSupplier<SpanTracker> spanTrackerSupplier,
      OptionalConfigurableSupplier<TraceThreadChangeDetector> traceThreadChangeDetectorSupplier,
      OptionalConfigurableSupplier<SnapshotProfilingSpanProcessor> profilingSpanProcessorSupplier,
      AutoConfiguredOpenTelemetrySdk sdk,
      OtelLoggerFactory otelLoggerFactory) {
    this.configurationSupplier = configurationSupplier;
    this.commandQueue = commandQueue;
    this.stagingAreaSupplier = stagingAreaSupplier;
    this.stackTraceSamplerSupplier = stackTraceSamplerSupplier;
    this.stackTraceExporterSupplier = stackTraceExporterSupplier;
    this.spanTrackerSupplier = spanTrackerSupplier;
    this.traceThreadChangeDetectorSupplier = traceThreadChangeDetectorSupplier;
    this.profilingSpanProcessorSupplier = profilingSpanProcessorSupplier;
    this.sdk = sdk;
    this.otelLoggerFactory = otelLoggerFactory;
  }

  public static SnapshotProfilingSupervisor initialize(AutoConfiguredOpenTelemetrySdk sdk) {
    if (SUPPLIER.isConfigured()) {
      throw new IllegalStateException("Snapshot profiling already initialized");
    }

    ExecutorService executor =
        HelpfulExecutors.newSingleThreadExecutor("Snapshot Profiling Supervisor");
    BlockingQueue<ProfilingCommand> queue = new LinkedBlockingQueue<>();
    SnapshotProfilingSupervisor supervisor =
        new SnapshotProfilingSupervisor(
            SnapshotProfilingConfiguration.SUPPLIER,
            queue,
            StagingArea.SUPPLIER,
            StackTraceSampler.SUPPLIER,
            StackTraceExporter.SUPPLIER,
            SpanTracker.SUPPLIER,
            TraceThreadChangeDetector.SUPPLIER,
            SnapshotProfilingSpanProcessor.SUPPLIER,
            sdk,
            new OtelLoggerFactory());
    SUPPLIER.configure(supervisor);
    supervisor.start(executor);

    return supervisor;
  }

  @VisibleForTesting
  void start(ExecutorService executor) {
    executor.submit(this::commandLoop);
  }

  private void commandLoop() {
    while (true) {
      try {
        ProfilingCommand command = commandQueue.take();
        handleCommand(command);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        logger.fine("SnapshotProfilingSupervisor is shutting down");
        return;
      } catch (Exception e) {
        logger.log(WARNING, "SnapshotProfilingSupervisor encountered an unexpected exception", e);
      }
    }
  }

  public void requestStartProfiling() {
    commandQueue.add(ProfilingCommand.START);
  }

  public void requestStopProfiling() {
    commandQueue.add(ProfilingCommand.STOP);
  }

  public void requestReinitializeProfiling() {
    commandQueue.add(ProfilingCommand.REINITIALIZE);
  }

  @VisibleForTesting
  boolean isRunning() {
    return running;
  }

  private void handleCommand(ProfilingCommand command) {
    switch (command) {
      case START:
        tryStart();
        break;
      case STOP:
        tryStop();
        break;
      case REINITIALIZE:
        tryReinitialize();
        break;
    }
  }

  private void tryStart() {
    if (running) {
      return;
    }

    SnapshotProfilingConfiguration configuration = configurationSupplier.get();
    configuration.log();

    // Create a new components
    stagingAreaSupplier.configure(createStagingArea(configuration));
    stackTraceSamplerSupplier.configure(createStackTraceSampler(configuration));
    stackTraceExporterSupplier.configure(createStackTraceExporter(configuration));

    // Enable components created during SDK initialization
    spanTrackerSupplier.get().setEnabled(true);
    traceThreadChangeDetectorSupplier.get().setEnabled(true);
    profilingSpanProcessorSupplier.get().setEnabled(true);

    running = true;
    logger.info("Snapshot profiling is active.");
  }

  private void tryStop() {
    if (!running) {
      return;
    }

    // Dispose components that can be recreated
    stackTraceSamplerSupplier.get().close();
    stackTraceSamplerSupplier.reset();

    stagingAreaSupplier.get().close();
    stagingAreaSupplier.reset();

    stackTraceExporterSupplier.get().close();
    stackTraceExporterSupplier.reset();

    // Disable components created during SDK initialization
    spanTrackerSupplier.get().setEnabled(false);
    traceThreadChangeDetectorSupplier.get().setEnabled(false);
    profilingSpanProcessorSupplier.get().setEnabled(false);

    running = false;
    logger.info("Snapshot profiling is deactivated.");
  }

  private void tryReinitialize() {
    if (running) {
      tryStop();
    }

    if (configurationSupplier.get().isEnabled()) {
      tryStart();
    }
  }

  StagingArea createStagingArea(SnapshotProfilingConfiguration configuration) {
    Duration interval = configuration.getExportInterval();
    int capacity = configuration.getStagingCapacity();
    return new PeriodicallyExportingStagingArea(stackTraceExporterSupplier, interval, capacity);
  }

  StackTraceSampler createStackTraceSampler(SnapshotProfilingConfiguration configuration) {
    Duration samplingPeriod = configuration.getSamplingInterval();
    return new PeriodicStackTraceSampler(stagingAreaSupplier, spanTrackerSupplier, samplingPeriod);
  }

  StackTraceExporter createStackTraceExporter(SnapshotProfilingConfiguration configuration) {
    Resource resource = AutoConfigureUtil.getResource(sdk);
    io.opentelemetry.api.logs.Logger otelLogger =
        buildLogger(otelLoggerFactory, resource, configuration.getConfigProperties());

    return new AsyncStackTraceExporter(otelLogger, configuration.getStackDepth());
  }

  private io.opentelemetry.api.logs.Logger buildLogger(
      OtelLoggerFactory otelLoggerFactory, Resource resource, Object configProperties) {
    if (configProperties instanceof DeclarativeConfigProperties) {
      DeclarativeConfigProperties exporterConfig =
          DeclarativeConfigPropertiesUtil.getStructuredOrEmpty(
              (DeclarativeConfigProperties) configProperties, "exporter");
      return otelLoggerFactory.build(exporterConfig, resource);
    }
    if (configProperties instanceof ConfigProperties) {
      return otelLoggerFactory.build((ConfigProperties) configProperties, resource);
    }
    throw new IllegalArgumentException(
        "Unsupported config properties type: " + configProperties.getClass().getName());
  }

  enum ProfilingCommand {
    START,
    STOP,
    REINITIALIZE
  }
}
