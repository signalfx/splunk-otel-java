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
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class oversees the profiling subsystem. It runs for the entire time that the agent is
 * running.
 */
public class ProfilingSupervisor {
  static {
    setupContextStorage();
  }

  private static final java.util.logging.Logger logger =
      java.util.logging.Logger.getLogger(ProfilingSupervisor.class.getName());
  private final ProfilerConfiguration config;
  private final JFR jfr;
  private final AutoConfiguredOpenTelemetrySdk sdk;
  private final BlockingQueue<ProfilingCommand> commandQueue;
  private final AtomicReference<PeriodicRecordingFlusher> sequencer = new AtomicReference<>();

  @VisibleForTesting
  ProfilingSupervisor(
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
    ExecutorService executor = HelpfulExecutors.newSingleThreadExecutor("JFR Profiler");
    // TODO: What if already started?
    BlockingQueue<ProfilingCommand> queue = new LinkedBlockingQueue<>();
    ProfilingSupervisor supervisor = new ProfilingSupervisor(config, JFR.getInstance(), sdk, queue);
    supervisor.start(executor);
    return supervisor;
  }

  @VisibleForTesting
  void start(ExecutorService executor) {
    executor.submit(this::forever);
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
        disableContextStorage();
        logger.warning("ProfilingSupervisor STOP not yet implemented");
        break;
    }
  }

  private void disableContextStorage() {
    JfrContextStorage.setEnabled(false);
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
    enableContextStorage();
    activateJfrAndRunUntilStopped(getResource(sdk));
  }

  private void enableContextStorage() {
    JfrContextStorage.setEnabled(true);
  }

  private boolean alreadyRunning() {
    return sequencer.get() != null;
  }

  private void activateJfrAndRunUntilStopped(Resource resource) {
    PeriodicRecordingFlusher periodicRecordingFlusher =
        makeRecordingSequencerBuilder(resource).jfr(jfr).build();
    if (sequencer.compareAndSet(null, periodicRecordingFlusher)) {
      periodicRecordingFlusher.start();
    }
  }

  // Exists for testing
  RecordingSequencerBuilder makeRecordingSequencerBuilder(Resource resource) {
    return PeriodicRecordingFlusher.builder(config, resource);
  }

  private static void setupContextStorage() {
    ContextStorage.addWrapper(JfrContextStorage::new);
  }

  enum ProfilingCommand {
    START,
    STOP
  }
}
