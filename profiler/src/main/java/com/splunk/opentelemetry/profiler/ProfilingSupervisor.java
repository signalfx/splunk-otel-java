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

import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getResource;
import static java.util.logging.Level.FINE;

import com.google.common.annotations.VisibleForTesting;
import com.splunk.opentelemetry.profiler.util.HelpfulExecutors;
import com.splunk.opentelemetry.profiler.util.OptionalConfigurableSupplier;
import io.opentelemetry.context.ContextStorage;
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

  private final OptionalConfigurableSupplier<ProfilerConfiguration> configSupplier;
  private final JFR jfr;
  private final AutoConfiguredOpenTelemetrySdk sdk;
  private final BlockingQueue<ProfilingCommand> commandQueue;
  private final AtomicReference<PeriodicRecordingFlusher> recordingFlusher =
      new AtomicReference<>();
  private static final AtomicReference<JfrContextStorage> jfrContextStorage =
      new AtomicReference<>();
  private static final AtomicBoolean contextStorageSetup = new AtomicBoolean();

  @VisibleForTesting
  ProfilingSupervisor(
      OptionalConfigurableSupplier<ProfilerConfiguration> configSupplier,
      JFR jfr,
      AutoConfiguredOpenTelemetrySdk sdk,
      BlockingQueue<ProfilingCommand> commandQueue) {
    this.configSupplier = configSupplier;
    this.jfr = jfr;
    this.sdk = sdk;
    this.commandQueue = commandQueue;
  }

  static ProfilingSupervisor createAndStart(
      AutoConfiguredOpenTelemetrySdk sdk,
      OptionalConfigurableSupplier<ProfilerConfiguration> configSupplier) {
    if (SUPPLIER.isConfigured()) {
      throw new IllegalStateException("Already started");
    }
    ExecutorService executor = HelpfulExecutors.newSingleThreadExecutor("JFR Profiler");
    BlockingQueue<ProfilingCommand> queue = new LinkedBlockingQueue<>();
    ProfilingSupervisor supervisor =
        new ProfilingSupervisor(configSupplier, JFR.getInstance(), sdk, queue);
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
        logger.fine("ProfilingSupervisor is shutting down");
        return;
      } catch (Exception e) {
        logger.log(FINE, "ProfilingSupervisor encountered an unexpected exception", e);
      }
    }
  }

  public void requestStartProfiling() {
    commandQueue.add(ProfilingCommand.START);
  }

  public void requestStopProfiling() {
    commandQueue.add(ProfilingCommand.STOP);
  }

  private void handleCommand(ProfilingCommand command) {
    switch (command) {
      case START:
        tryStart();
        break;
      case STOP:
        tryStop();
        break;
    }
  }

  private void setJfrContextStorageEnabled(boolean enabled) {
    JfrContextStorage contextStorage = jfrContextStorage.get();
    if (contextStorage != null) {
      contextStorage.setEnabled(enabled);
    }
  }

  /**
   * Try and start the profiler. This does not check configuration, just responds to a command
   * request.
   */
  private void tryStart() {
    if (isJfrRecordingActive()) {
      logger.warning("JFR is already running, not starting again.");
      return;
    }
    if (!jfr.isAvailable()) {
      logger.warning(
          "JDK Flight Recorder (JFR) is not available in this JVM. Profiling will not start.");
      return;
    }
    configSupplier.get().log();
    setJfrContextStorageEnabled(true);
    activateJfrRecording(getResource(sdk));
    logger.info("Profiler is active.");
  }

  private void tryStop() {
    if (!isJfrRecordingActive()) {
      logger.warning("JFR is not running already, not stopping again.");
      return;
    }
    setJfrContextStorageEnabled(false);
    deactivateJfrRecording();
    logger.info("Profiler is deactivated.");
  }

  private boolean isJfrRecordingActive() {
    return recordingFlusher.get() != null;
  }

  private void activateJfrRecording(Resource resource) {
    PeriodicRecordingFlusher recordingFlusher =
        makeRecordingFlusherBuilder(resource).jfr(jfr).build();
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

  // Exists for testing
  PeriodicRecordingFlusherBuilder makeRecordingFlusherBuilder(Resource resource) {
    return PeriodicRecordingFlusher.builder(configSupplier.get(), resource);
  }

  static void setupJfrContextStorage() {
    if (!contextStorageSetup.compareAndSet(false, true)) {
      return;
    }

    ContextStorage.addWrapper(
        (delegate) -> {
          JfrContextStorage storage = new JfrContextStorage(delegate);
          jfrContextStorage.set(storage);
          return storage;
        });
  }

  enum ProfilingCommand {
    START,
    STOP
  }
}
