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

// Includes work from:
/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.splunk.opentelemetry.jmx;

import static io.opentelemetry.instrumentation.api.incubator.config.internal.SelectorConfig.Stability.STABLE;
import static java.util.Collections.emptyList;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.instrumentation.api.config.IncludeExclude;
import io.opentelemetry.instrumentation.api.incubator.config.internal.SelectorConfig;
import io.opentelemetry.instrumentation.jmx.JmxTelemetry;
import io.opentelemetry.instrumentation.jmx.JmxTelemetryBuilder;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Temporary adapter for the package-private setup in upstream {@code JmxMetricInsightInstaller}.
 * Keep this in sync with instrumentation/jmx-metrics/javaagent until upstream supports inline
 * rules, then remove it together with the distribution-specific startup wiring.
 */
final class UpstreamJmxConfiguration {
  private static final Logger logger = Logger.getLogger(UpstreamJmxConfiguration.class.getName());

  private UpstreamJmxConfiguration() {}

  static JmxTelemetryBuilder createBuilder(
      OpenTelemetry openTelemetry, DeclarativeConfigProperties config) {
    JmxTelemetryBuilder builder =
        JmxTelemetry.builder(openTelemetry)
            .beanDiscoveryDelay(
                Duration.ofMillis(
                    config.get("discovery").getLong("delay", Duration.ofMinutes(1).toMillis())));

    config.getScalarList("config", String.class, emptyList()).stream()
        .map(Paths::get)
        .forEach(path -> {
          try {
            builder.addRules(path);
          } catch (RuntimeException e) {
            // Match upstream: a bad external file must not prevent other rules from being loaded.
            logger.log(Level.SEVERE, "Error while loading JMX configuration from " + path, e);
          }
        });

    List<String> unstableInclude =
        config
            .get("metrics")
            .get("experimental")
            .getScalarList("included", String.class, emptyList());
    // Bundled JVM rules overlap with runtime-telemetry.
    builder.setInternalMetricsSystemFilter(IncludeExclude.builder().setExcluded("jvm").build());
    builder.setInternalMetricsUnstableMetricsFilter(
        unstableInclude.isEmpty()
            ? IncludeExclude.builder().setExcluded("*").build()
            : IncludeExclude.builder().setIncluded(unstableInclude).build());

    IncludeExclude metrics = SelectorConfig.resolve(config, "jmx", "metrics", STABLE);
    if (metrics != null) {
      builder.setMetrics(metrics);
    }
    return builder;
  }
}
