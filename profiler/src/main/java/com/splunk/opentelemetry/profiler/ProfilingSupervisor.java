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
import static java.util.logging.Level.WARNING;

import com.google.common.annotations.VisibleForTesting;
import com.splunk.opentelemetry.instrumentation.jvmmetrics.otel.OtelAllocatedMemoryMetrics;
import com.splunk.opentelemetry.instrumentation.jvmmetrics.otel.OtelGcMemoryMetrics;
import com.splunk.opentelemetry.profiler.util.HelpfulExecutors;
import com.splunk.opentelemetry.profiler.util.OptionalConfigurableSupplier;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class oversees the profiling subsystem. It runs for the entire time that the agent is
 * running.
 */
public class ProfilingSupervisor {
  public static final OptionalConfigurableSupplier<ProfilingSupervisor> SUPPLIER =
      new OptionalConfigurableSupplier<>();
  private static final java.util.logging.Logger logger =
      java.util.logging.Logger.getLogger(ProfilingSupervisor.class.getName());
  private static final String JVM_METRICS_ENABLED_CONFIG_KEY =
      "otel.instrumentation.jvm-metrics-splunk.enabled";

  private final OptionalConfigurableSupplier<ProfilerConfiguration> configSupplier;
  private final JFR jfr;
  private final AutoConfiguredOpenTelemetrySdk sdk;
  private final BlockingQueue<ProfilingCommand> commandQueue = new LinkedBlockingQueue<>();
  private final ProfilerFactory profilerFactory;
  private final OtelAllocatedMemoryMetrics allocatedMemoryMetrics;
  private final OtelGcMemoryMetrics gcMemoryMetrics;
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final AtomicReference<PeriodicRecordingFlusher> recordingFlusher =
      new AtomicReference<>();
  private final AtomicReference<JavaProfiler> javaProfiler = new AtomicReference<>();
  private static final AtomicReference<ProfilerContextStorage> profilerContextStorage =
      new AtomicReference<>();
  private static final AtomicBoolean contextStorageSetup = new AtomicBoolean();

  @VisibleForTesting
  ProfilingSupervisor(
      OptionalConfigurableSupplier<ProfilerConfiguration> configSupplier,
      JFR jfr,
      AutoConfiguredOpenTelemetrySdk sdk,
      ProfilerFactory profilerFactory,
      OtelAllocatedMemoryMetrics allocatedMemoryMetrics,
      OtelGcMemoryMetrics gcMemoryMetrics) {
    this.configSupplier = configSupplier;
    this.jfr = jfr;
    this.sdk = sdk;
    this.profilerFactory = profilerFactory;
    this.allocatedMemoryMetrics = allocatedMemoryMetrics;
    this.gcMemoryMetrics = gcMemoryMetrics;
  }

  static ProfilingSupervisor createAndStart(AutoConfiguredOpenTelemetrySdk sdk) {
    if (SUPPLIER.isConfigured()) {
      throw new IllegalStateException("Already started");
    }
    ExecutorService executor = HelpfulExecutors.newSingleThreadExecutor("Splunk Profiler");
    ProfilingSupervisor supervisor =
        new ProfilingSupervisor(
            ProfilerConfiguration.SUPPLIER,
            JFR.getInstance(),
            sdk,
            new ProfilerFactory(),
            new OtelAllocatedMemoryMetrics(),
            new OtelGcMemoryMetrics());
    SUPPLIER.configure(supervisor);
    supervisor.start(executor);
    supervisor.updateJvmMemoryMetrics();

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
        logger.fine("ProfilingSupervisor is shutting down");
        return;
      } catch (Exception e) {
        logger.log(WARNING, "ProfilingSupervisor encountered an unexpected exception", e);
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

  private void setJfrContextStorageEnabled(
      boolean enabled, boolean emitJrfContextEvents, boolean trackActiveContext) {
    ProfilerContextStorage contextStorage = profilerContextStorage.get();
    if (contextStorage != null) {
      contextStorage.setEnabled(enabled);
      contextStorage.setEmitJfrEvents(emitJrfContextEvents);
      contextStorage.setTrackActiveContext(trackActiveContext);
    }
  }

  /**
   * Try and start the profiler. This does not check configuration, just responds to a command
   * request.
   */
  private void tryStart() {
    if (started.get()) {
      logger.fine("Profiler is already running, not starting again.");
      return;
    }

    ProfilerConfiguration config = configSupplier.get();
    ProfilerConfiguration.CpuProfilingMode cpuProfilingMode = config.getCpuProfilingMode();
    boolean jfrUsed =
        cpuProfilingMode == ProfilerConfiguration.CpuProfilingMode.JFR || config.getMemoryEnabled();
    if (jfrUsed && !jfr.isAvailable()) {
      logger.warning(
          "JDK Flight Recorder (JFR) is not available in this JVM. Profiling will not start.");
      return;
    }

    config.log();
    updateJvmMemoryMetrics();
    setJfrContextStorageEnabled(
        true, jfrUsed, cpuProfilingMode == ProfilerConfiguration.CpuProfilingMode.JAVA);
    if (jfrUsed) {
      activateJfrRecording(getResource(sdk));
    }
    if (cpuProfilingMode == ProfilerConfiguration.CpuProfilingMode.JAVA) {
      activateJavaCpuProfiler(getResource(sdk));
    }
    started.set(true);
    logger.info("Profiler is active.");
  }

  private void tryStop() {
    if (!started.get()) {
      logger.fine("Profiler is not running, not stopping again.");
      return;
    }
    setJfrContextStorageEnabled(false, false, false);
    deactivateJfrRecording();
    deactivateJavaCpuProfiler();
    started.set(false);
    logger.info("Profiler is deactivated.");
  }

  private void tryReinitialize() {
    updateJvmMemoryMetrics();
    tryStop();
    // Start the profiler with current settings if it is enabled. New settings will be applied.
    if (configSupplier.get().isEnabled()) {
      tryStart();
    }
  }

  private void activateJfrRecording(Resource resource) {
    PeriodicRecordingFlusher recordingFlusher =
        profilerFactory.createJfrProfiler(configSupplier.get(), resource, jfr);
    if (this.recordingFlusher.compareAndSet(null, recordingFlusher)) {
      recordingFlusher.start();
    }
  }

  private void deactivateJfrRecording() {
    PeriodicRecordingFlusher recordingFlusher = this.recordingFlusher.getAndSet(null);
    if (recordingFlusher != null) {
      recordingFlusher.stop();
    }
  }

  private void activateJavaCpuProfiler(Resource resource) {
    JavaProfiler javaProfiler =
        profilerFactory.createJavaProfiler(
            configSupplier.get(), resource, profilerContextStorage.get());
    if (this.javaProfiler.compareAndSet(null, javaProfiler)) {
      javaProfiler.start();
    }
  }

  private void deactivateJavaCpuProfiler() {
    JavaProfiler javaProfiler = this.javaProfiler.getAndSet(null);
    if (javaProfiler != null) {
      javaProfiler.stop();
    }
  }

  private void updateJvmMemoryMetrics() {
    if (isJvmMemoryMetricsEnabled()) {
      allocatedMemoryMetrics.install();
      gcMemoryMetrics.install();
    } else {
      allocatedMemoryMetrics.uninstall();
      gcMemoryMetrics.uninstall();
    }
  }

  private boolean isJvmMemoryMetricsEnabled() {
    ProfilerConfiguration config = configSupplier.get();
    if (AutoConfigureUtil.isDeclarativeConfig(sdk)) {
      return AutoConfigureUtil.getConfigProvider(sdk)
          .getInstrumentationConfig()
          .get("java")
          .getStructured("jvm-metrics-splunk", empty())
          .getBoolean("enabled", config.getMemoryEnabled());
    }

    return AutoConfigureUtil.getConfig(sdk)
        .getBoolean(JVM_METRICS_ENABLED_CONFIG_KEY, config.getMemoryEnabled());
  }

  static void setupJfrContextStorage() {
    if (!contextStorageSetup.compareAndSet(false, true)) {
      return;
    }

    ContextStorage.addWrapper(
        (delegate) -> {
          ProfilerContextStorage storage = new ProfilerContextStorage(delegate);
          profilerContextStorage.set(storage);
          return storage;
        });
  }

  enum ProfilingCommand {
    START,
    STOP,
    REINITIALIZE
  }
}
