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

import static io.opentelemetry.api.incubator.config.DeclarativeConfigProperties.empty;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.KeyValue;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.instrumentation.api.config.IncludeExclude;
import io.opentelemetry.instrumentation.jmx.JmxTelemetry;
import io.opentelemetry.instrumentation.jmx.JmxTelemetryBuilder;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Collects inline JMX metrics using an existing SDK, independently of agent startup. */
public final class JmxMetricsService implements AutoCloseable {
  private final OpenTelemetry openTelemetry;
  private AutoCloseable collection;

  public JmxMetricsService(OpenTelemetry openTelemetry) {
    this.openTelemetry = requireNonNull(openTelemetry, "openTelemetry");
  }

  /**
   * Parses inline rules and starts local collection through the supplied SDK's metric readers.
   *
   * <p>Repeated calls while running leave the current collection unchanged. Close this service
   * before starting it with different rules. The caller owns its lifecycle, including shutdown.
   *
   * @param jmxConfig the {@code distribution.splunk.java.jmx} node containing {@code rules}
   * @param instrumentationConfig the {@code instrumentation/development.java.jmx} node containing
   *     discovery and filtering settings; may be {@link DeclarativeConfigProperties#empty()}
   * @return whether collection is running; false if no rules are configured
   * @throws IllegalArgumentException if the rules or settings are invalid
   */
  public synchronized boolean start(
      DeclarativeConfigProperties jmxConfig, DeclarativeConfigProperties instrumentationConfig) {
    if (collection != null) {
      return true;
    }

    List<DeclarativeConfigProperties> rules = jmxConfig.getStructuredList("rules", emptyList());
    if (rules.isEmpty()) {
      return false;
    }

    DeclarativeConfigProperties metrics = instrumentationConfig.getStructured("metrics", empty());
    JmxTelemetryBuilder builder =
        JmxTelemetry.builder(openTelemetry)
            .beanDiscoveryDelay(
                Duration.ofMillis(
                    instrumentationConfig
                        .getStructured("discovery", empty())
                        .getLong("delay", Duration.ofMinutes(1).toMillis())))
            .setMetrics(
                IncludeExclude.builder()
                    .setIncluded(metrics.getScalarList("included", String.class, emptyList()))
                    .setExcluded(metrics.getScalarList("excluded", String.class, emptyList()))
                    .build());

    List<Map<String, Object>> ruleMaps =
        rules.stream().map(DeclarativeConfigProperties::toMap).collect(toList());
    // JSON is valid YAML; use the existing OpenTelemetry API to encode it for the upstream parser.
    String json = toValue(singletonMap("rules", ruleMaps)).asString();
    builder.addRules(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

    collection = builder.build().start();
    return true;
  }

  /** Stops discovery and unregisters metric callbacks. May be called more than once. */
  @Override
  public synchronized void close() throws Exception {
    if (collection != null) {
      try {
        collection.close();
      } finally {
        collection = null;
      }
    }
  }

  private static Value<?> toValue(Object value) {
    if (value == null) {
      return Value.empty();
    }
    if (value instanceof String) {
      return Value.of((String) value);
    }
    if (value instanceof Boolean) {
      return Value.of((Boolean) value);
    }
    if (value instanceof Long) {
      return Value.of((Long) value);
    }
    if (value instanceof Double) {
      return Value.of((Double) value);
    }
    if (value instanceof List<?>) {
      return Value.of(((List<?>) value).stream().map(JmxMetricsService::toValue).collect(toList()));
    }
    if (value instanceof Map<?, ?>) {
      return Value.of(
          ((Map<?, ?>) value)
              .entrySet().stream()
                  .map(entry -> KeyValue.of((String) entry.getKey(), toValue(entry.getValue())))
                  .toArray(KeyValue[]::new));
    }
    throw new IllegalArgumentException(
        "Unsupported JMX rule value type: " + value.getClass().getName());
  }
}
