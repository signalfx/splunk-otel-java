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

package com.splunk.opentelemetry.jmx;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.javaagent.extension.instrumentation.internal.AgentDistributionConfig;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

/** Owns distribution JMX collection across startup and synchronous remote configuration updates. */
public class JmxMetricInsightSupervisor implements AutoCloseable {
  private static final AtomicReference<JmxMetricInsightSupervisor> instance =
      new AtomicReference<>();

  private final JmxMetricsService metricsService;
  private final boolean enabled;
  @Nullable private DeclarativeConfigProperties configuration;
  @Nullable private Map<String, Object> configurationSnapshot;
  private boolean running;
  private boolean closed;

  @VisibleForTesting
  JmxMetricInsightSupervisor(
      JmxMetricsService metricsService,
      @Nullable DeclarativeConfigProperties startupConfiguration) {
    this.metricsService = requireNonNull(metricsService, "metricsService");
    enabled = AgentDistributionConfig.get().isInstrumentationEnabled("jmx");
    configuration = startupConfiguration;
    // TODO: This will be refactored when effective config reporting is implemented for JMX
    // For now null is stored for env vars configuration
    configurationSnapshot =
        startupConfiguration == null || startupConfiguration.getPropertyKeys().isEmpty()
            ? null
            : DeclarativeConfigProperties.toMap(startupConfiguration);
  }

  public static JmxMetricInsightSupervisor initialize(
      OpenTelemetry openTelemetry, @Nullable DeclarativeConfigProperties startupConfiguration) {
    JmxMetricInsightSupervisor supervisor =
        new JmxMetricInsightSupervisor(new JmxMetricsService(openTelemetry), startupConfiguration);
    if (!instance.compareAndSet(null, supervisor)) {
      throw new IllegalStateException("JMX supervisor is already initialized");
    }
    return supervisor;
  }

  /** Returns the supervisor published by the JMX startup listener before OpAMP starts. */
  public static JmxMetricInsightSupervisor getInstance() {
    JmxMetricInsightSupervisor supervisor = instance.get();
    if (supervisor == null) {
      throw new IllegalStateException("JMX supervisor is not initialized");
    }
    return supervisor;
  }

  /** Returns whether this supervisor permits JMX collection. */
  public synchronized boolean isEnabled() {
    return enabled && configuration != null;
  }

  /** Starts collection if JMX was enabled at startup and a configuration is present. */
  public synchronized void start() {
    checkOpen();
    if (isEnabled()) {
      running = metricsService.start(configuration);
    }
  }

  /**
   * Replaces the current configuration with a complete {@code distribution.splunk.java.jmx} node.
   * Omitted settings use their defaults; an empty node resets collection to defaults. Identical
   * configurations leave the running collection alone. Invalid rules leave the previous collection
   * active and propagate to the remote processor so it can report failure. The first remote update
   * replaces startup settings whose property keys cannot be enumerated.
   */
  public synchronized void applyConfiguration(DeclarativeConfigProperties update) throws Exception {
    checkOpen();
    if (!enabled) {
      throw new IllegalStateException("JMX instrumentation was disabled at agent startup");
    }
    if (configuration == null) {
      throw new IllegalStateException(
          "Remote JMX updates require distribution.splunk.java.jmx to be enabled at agent startup");
    }

    Map<String, Object> updated =
        DeclarativeConfigProperties.toMap(requireNonNull(update, "update"));
    if (running && updated.equals(configurationSnapshot)) {
      return;
    }

    DeclarativeConfigProperties updatedConfiguration =
        DeclarativeConfiguration.toConfigProperties(updated);
    try {
      metricsService.reconfigure(updatedConfiguration);
    } catch (Exception e) {
      // Validation failures keep the old collection; a failure after stopping it does not.
      // Track the actual state so a later retry is not mistaken for an unchanged active config.
      running = metricsService.isRunning();
      throw e;
    }
    configuration = updatedConfiguration;
    configurationSnapshot = updated;
    running = true;
  }

  @Override
  public synchronized void close() throws Exception {
    if (closed) {
      return;
    }
    try {
      metricsService.close();
    } finally {
      running = false;
      closed = true;
      instance.compareAndSet(this, null);
    }
  }

  private void checkOpen() {
    if (closed) {
      throw new IllegalStateException("JMX supervisor is closed");
    }
  }
}
