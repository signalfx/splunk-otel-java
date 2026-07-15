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
import com.splunk.opentelemetry.profiler.util.OptionalConfigurableSupplier;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.util.logging.Logger;

public class SnapshotProfilingSupervisor {
  public static final OptionalConfigurableSupplier<SnapshotProfilingSupervisor> SUPPLIER =
      new OptionalConfigurableSupplier<>();
  private static final Logger logger =
      Logger.getLogger(SnapshotProfilingSupervisor.class.getName());

  private final OptionalConfigurableSupplier<SnapshotProfilingConfiguration> configurationSupplier;
  private final AutoConfiguredOpenTelemetrySdk sdk;
  private final OtelLoggerFactory otelLoggerFactory;
  private boolean running;

  @VisibleForTesting
  SnapshotProfilingSupervisor(
      OptionalConfigurableSupplier<SnapshotProfilingConfiguration> configurationSupplier,
      AutoConfiguredOpenTelemetrySdk sdk,
      OtelLoggerFactory otelLoggerFactory) {
    this.configurationSupplier = configurationSupplier;
    this.sdk = sdk;
    this.otelLoggerFactory = otelLoggerFactory;
  }

  public static SnapshotProfilingSupervisor initialize(AutoConfiguredOpenTelemetrySdk sdk) {
    if (SUPPLIER.isConfigured()) {
      throw new IllegalStateException("Snapshot profiling already initialized");
    }

    SnapshotProfilingSupervisor supervisor =
        new SnapshotProfilingSupervisor(
            SnapshotProfilingConfiguration.SUPPLIER, sdk, new OtelLoggerFactory());
    SUPPLIER.configure(supervisor);

    return supervisor;
  }

  public synchronized void startProfiling() {
    if (running) {
      return;
    }

    // TODO: Is it needed
    if (!configurationSupplier.get().isEnabled()) {
      throw new IllegalStateException("Snapshot profiling not enabled in configuration");
    }

    configurationSupplier.get().log();

    // StagingArea.SUPPLIER
    // StackTraceSampler.SUPPLIER
    // StackTraceExporter.SUPPLIER = AsyncStackTraceExporter
    StackTraceSamplerInitializer.setupStackTraceSampler(configurationSupplier.get());
    StackTraceSamplerInitializer.setupStackTraceExporter(
        configurationSupplier.get(), AutoConfigureUtil.getResource(sdk), otelLoggerFactory);

    // SpanTracker.SUPPLIER
    SpanTracker.SUPPLIER.get().setEnabled(true);
    // SnapshotProfilingSpanProcessorComponentProvider -> SnapshotProfilingSpanProcessor
    // SdkShutdownHookComponentProvider -> SdkShutdownHook (raczej nie wymaga zmian)

    running = true;
    logger.info("Snapshot profiling is active.");
  }

  public synchronized void stopProfiling() {
    if (!running) {
      return;
    }

    StackTraceSampler.SUPPLIER.get().close();
    StackTraceSampler.SUPPLIER.reset();

    StagingArea.SUPPLIER.get().close();
    StagingArea.SUPPLIER.reset();

    StackTraceExporter.SUPPLIER.get().close();
    StackTraceExporter.SUPPLIER.reset();

    SpanTracker.SUPPLIER.get().setEnabled(false);

    running = false;
  }
}
