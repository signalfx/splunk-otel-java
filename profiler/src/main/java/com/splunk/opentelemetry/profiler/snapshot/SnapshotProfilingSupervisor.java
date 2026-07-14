package com.splunk.opentelemetry.profiler.snapshot;

import com.google.common.annotations.VisibleForTesting;
import com.splunk.opentelemetry.profiler.OtelLoggerFactory;
import com.splunk.opentelemetry.profiler.ProfilingSupervisor;
import com.splunk.opentelemetry.profiler.util.OptionalConfigurableSupplier;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.util.logging.Logger;

public class SnapshotProfilingSupervisor {
  public static final OptionalConfigurableSupplier<SnapshotProfilingSupervisor> SUPPLIER =
      new OptionalConfigurableSupplier<>();
  private static final Logger logger = Logger.getLogger(SnapshotProfilingSupervisor.class.getName());

  private final OptionalConfigurableSupplier<SnapshotProfilingConfiguration> configurationSupplier;
  private final AutoConfiguredOpenTelemetrySdk sdk;
  private final OtelLoggerFactory otelLoggerFactory;
  private boolean running;

  @VisibleForTesting
  SnapshotProfilingSupervisor(
      OptionalConfigurableSupplier<SnapshotProfilingConfiguration> configurationSupplier,
      AutoConfiguredOpenTelemetrySdk sdk,
      OtelLoggerFactory otelLoggerFactory
      ) {
    this.configurationSupplier = configurationSupplier;
    this.sdk = sdk;
    this.otelLoggerFactory = otelLoggerFactory;
  }



  public static SnapshotProfilingSupervisor initialize(AutoConfiguredOpenTelemetrySdk sdk) {
    if (SUPPLIER.isConfigured()) {
      throw new IllegalStateException("Snapshot profiling already initialized");
    }

    SnapshotProfilingSupervisor supervisor = new SnapshotProfilingSupervisor(
        SnapshotProfilingConfiguration.SUPPLIER,
        sdk,
        new OtelLoggerFactory()
    );
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
        configurationSupplier.get(),
        AutoConfigureUtil.getResource(sdk),
        otelLoggerFactory
        );

    // SpanTracker.SUPPLIER
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

    running = false;
  }

}
