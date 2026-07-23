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

import com.google.common.annotations.VisibleForTesting;
import com.splunk.opentelemetry.profiler.OtelLoggerFactory;
import com.splunk.opentelemetry.profiler.util.DeclarativeConfigPropertiesUtil;
import com.splunk.opentelemetry.profiler.util.OptionalConfigurableSupplier;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Duration;
import java.util.logging.Logger;

public class SnapshotProfilingSupervisor {
  public static final OptionalConfigurableSupplier<SnapshotProfilingSupervisor> SUPPLIER =
      new OptionalConfigurableSupplier<>();
  private static final Logger logger =
      Logger.getLogger(SnapshotProfilingSupervisor.class.getName());

  private final OptionalConfigurableSupplier<SnapshotProfilingConfiguration> configurationSupplier;
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
  private boolean running;

  @VisibleForTesting
  SnapshotProfilingSupervisor(
      OptionalConfigurableSupplier<SnapshotProfilingConfiguration> configurationSupplier,
      ConfigurableSupplier<StagingArea> stagingAreaSupplier,
      ConfigurableSupplier<StackTraceSampler> stackTraceSamplerSupplier,
      ConfigurableSupplier<StackTraceExporter> stackTraceExporterSupplier,
      ConfigurableSupplier<SpanTracker> spanTrackerSupplier,
      OptionalConfigurableSupplier<TraceThreadChangeDetector> traceThreadChangeDetectorSupplier,
      OptionalConfigurableSupplier<SnapshotProfilingSpanProcessor> profilingSpanProcessorSupplier,
      AutoConfiguredOpenTelemetrySdk sdk,
      OtelLoggerFactory otelLoggerFactory) {
    this.configurationSupplier = configurationSupplier;
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

    SnapshotProfilingSupervisor supervisor =
        new SnapshotProfilingSupervisor(
            SnapshotProfilingConfiguration.SUPPLIER,
            StagingArea.SUPPLIER,
            StackTraceSampler.SUPPLIER,
            StackTraceExporter.SUPPLIER,
            SpanTracker.SUPPLIER,
            TraceThreadChangeDetector.SUPPLIER,
            SnapshotProfilingSpanProcessor.SUPPLIER,
            sdk,
            new OtelLoggerFactory());
    SUPPLIER.configure(supervisor);

    return supervisor;
  }

  public synchronized void startProfiling() {
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

  public synchronized void stopProfiling() {
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

  public synchronized void reinitializeProfiling() {
    if (running) {
      stopProfiling();
    }

    if (configurationSupplier.get().isEnabled()) {
      startProfiling();
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
}
