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

import static io.opentelemetry.instrumentation.api.incubator.config.internal.SelectorConfig.Stability.STABLE;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.SEVERE;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.instrumentation.api.config.IncludeExclude;
import io.opentelemetry.instrumentation.api.incubator.config.internal.SelectorConfig;
import io.opentelemetry.instrumentation.jmx.JmxTelemetry;
import io.opentelemetry.instrumentation.jmx.JmxTelemetryBuilder;
import io.opentelemetry.instrumentation.jmx.internal.Internal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

/**
 * Collects configured JMX metrics using an existing SDK, independently of startup.
 *
 * <p>This class is not thread-safe. Its owner must serialize access to its lifecycle and state.
 */
final class JmxMetricsService implements AutoCloseable {
  private static final Logger logger = Logger.getLogger(JmxMetricsService.class.getName());
  private static final String INSTRUMENTATION_NAME = "jmx";

  private final OpenTelemetry openTelemetry;
  private AutoCloseable jmxCollector;

  JmxMetricsService(OpenTelemetry openTelemetry) {
    this.openTelemetry = requireNonNull(openTelemetry, "openTelemetry");
  }

  /**
   * Parses inline rules and starts local collection through the supplied SDK's metric readers.
   *
   * <p>Repeated calls while running leave the current collection unchanged. Close this service
   * before starting it with different rules. The caller owns its lifecycle, including shutdown.
   *
   * @param jmxConfig the {@code instrumentation/development.java.jmx} node containing upstream JMX
   *     settings and optional inline {@code rules}
   * @throws IllegalArgumentException if the rules or settings are invalid
   */
  void start(DeclarativeConfigProperties jmxConfig) {
    if (jmxCollector != null) {
      logger.fine("Already started");
      return;
    }

    jmxCollector = createTelemetry(jmxConfig).start();
  }

  /** Validates replacement settings before stopping the current collection. */
  void reconfigure(DeclarativeConfigProperties jmxConfig) throws Exception {
    JmxTelemetry telemetry = createTelemetry(jmxConfig);
    close();
    jmxCollector = telemetry.start();
  }

  boolean isRunning() {
    return jmxCollector != null;
  }

  private JmxTelemetry createTelemetry(DeclarativeConfigProperties jmxConfig) {
    JmxTelemetryBuilder builder = createBuilder(openTelemetry, jmxConfig);
    configure(builder, jmxConfig);

    return builder.build();
  }

  private static JmxTelemetryBuilder createBuilder(
      OpenTelemetry openTelemetry, DeclarativeConfigProperties config) {
    return JmxTelemetry.builder(openTelemetry)
        .beanDiscoveryDelay(
            Duration.ofMillis(
                config.get("discovery").getLong("delay", Duration.ofMinutes(1).toMillis())));
  }

  /** Stops discovery and unregisters metric callbacks. May be called more than once. */
  @Override
  public void close() throws Exception {
    if (jmxCollector != null) {
      try {
        jmxCollector.close();
      } finally {
        jmxCollector = null;
      }
    }
  }

  // visible for testing
  // Implementation copied from upstream JmxMetricInsightInstaller class
  static void configure(JmxTelemetryBuilder jmx, DeclarativeConfigProperties config) {
    config.getScalarList("config", String.class, emptyList()).stream()
        .map(Paths::get)
        .forEach(path -> addFileRules(path, jmx));

    addInlineRules(config, jmx);

    List<String> unstableInclude =
        config
            .get("metrics")
            .get("experimental")
            .getScalarList("included", String.class, emptyList());
    // JVM rules overlap with runtime-telemetry.
    jmx.setInternalMetricsSystemFilter(IncludeExclude.builder().setExcluded("jvm").build());
    jmx.setInternalMetricsUnstableMetricsFilter(
        unstableInclude.isEmpty()
            ? IncludeExclude.builder().setExcluded("*").build()
            : IncludeExclude.builder().setIncluded(unstableInclude).build());

    // include/exclude metrics by name
    IncludeExclude metrics =
        SelectorConfig.resolve(config, INSTRUMENTATION_NAME, "metrics", STABLE);
    if (metrics != null) {
      jmx.setMetrics(metrics);
    }
  }

  private static void addInlineRules(DeclarativeConfigProperties config, JmxTelemetryBuilder jmx) {
    for (DeclarativeConfigProperties rule : config.getStructuredList("rules", emptyList())) {
      try {
        Internal.addRule(jmx, DeclarativeConfigProperties.toMap(rule));
      } catch (RuntimeException e) {
        logger.log(SEVERE, "Error while loading an inline JMX rule", e);
      }
    }
  }

  private static void addFileRules(Path path, JmxTelemetryBuilder builder) {
    try {
      builder.addRules(path);
    } catch (RuntimeException e) {
      // for now only log JMX metric configuration errors as they do not prevent agent startup
      logger.log(SEVERE, "Error while loading JMX configuration from " + path, e);
    }
  }
}
