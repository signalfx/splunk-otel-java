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

package com.splunk.opentelemetry;

import com.google.auto.service.AutoService;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.instrumentation.api.config.ConfigBuilder;
import io.opentelemetry.javaagent.extension.config.ConfigCustomizer;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(ConfigCustomizer.class)
public class SplunkConfiguration implements ConfigCustomizer {
  private static final Logger log = LoggerFactory.getLogger(SplunkConfiguration.class);

  public static final String SPLUNK_ACCESS_TOKEN = "splunk.access.token";
  public static final String OTEL_EXPORTER_JAEGER_ENDPOINT = "otel.exporter.jaeger.endpoint";
  public static final String PROFILER_ENABLED_PROPERTY = "splunk.profiler.enabled";
  public static final String PROFILER_MEMORY_ENABLED_PROPERTY = "splunk.profiler.memory.enabled";
  public static final String SPLUNK_REALM_PROPERTY = "splunk.realm";
  public static final String SPLUNK_REALM_NONE = "none";

  public static final String METRICS_ENABLED_PROPERTY = "splunk.metrics.enabled";
  public static final String METRICS_ENDPOINT_PROPERTY = "splunk.metrics.endpoint";
  public static final String METRICS_EXPORT_INTERVAL_PROPERTY = "splunk.metrics.export.interval";
  public static final String METRICS_IMPLEMENTATION = "splunk.metrics.implementation";

  @Override
  public Map<String, String> defaultProperties() {
    Map<String, String> config = new HashMap<>();

    config.put("otel.traces.sampler", "always_on");

    // disable logging instrumentations - we're not currently sending logs (yet)
    config.put("otel.instrumentation.java-util-logging.enabled", "false");
    config.put("otel.instrumentation.jboss-logmanager.enabled", "false");
    config.put("otel.instrumentation.log4j-appender.enabled", "false");
    config.put("otel.instrumentation.logback-appender.enabled", "false");

    // enable spring batch instrumentation
    config.put("otel.instrumentation.spring-batch.enabled", "true");
    config.put("otel.instrumentation.spring-batch.item.enabled", "true");

    return config;
  }

  @Override
  public Config customize(Config config) {
    ConfigBuilder builder = config.toBuilder();

    String metricsImplementation = config.getString(METRICS_IMPLEMENTATION);
    if (metricsImplementation == null) {
      metricsImplementation = "micrometer";
      builder.addProperty(METRICS_IMPLEMENTATION, "micrometer");
    }

    if ("micrometer".equals(metricsImplementation)) {
      // TODO: warn that micrometer metrics are deprecated
      // log.info(
      //     "Micrometer metrics are deprecated, OpenTelemetry metrics will become the default
      // implementation in the next release.");

      // by default no otel metrics are exported
      addIfAbsent(builder, config, "otel.metrics.exporter", "none");

      // disable metrics instrumentations, we're still on the micrometer based implementation
      addIfAbsent(builder, config, "otel.instrumentation.apache-dbcp.enabled", "false");
      addIfAbsent(builder, config, "otel.instrumentation.c3p0.enabled", "false");
      addIfAbsent(builder, config, "otel.instrumentation.hikaricp.enabled", "false");
      addIfAbsent(builder, config, "otel.instrumentation.micrometer.enabled", "false");
      addIfAbsent(builder, config, "otel.instrumentation.oracle-ucp.enabled", "false");
      addIfAbsent(builder, config, "otel.instrumentation.oshi.enabled", "false");
      addIfAbsent(builder, config, "otel.instrumentation.runtime-metrics.enabled", "false");
      addIfAbsent(builder, config, "otel.instrumentation.tomcat-jdbc.enabled", "false");
      addIfAbsent(builder, config, "otel.instrumentation.vibur-dbcp.enabled", "false");
    } else if ("opentelemetry".equals(metricsImplementation)) {
      String splunkMetricsEndpoint = config.getString(METRICS_ENDPOINT_PROPERTY);
      if (splunkMetricsEndpoint != null) {
        log.warn(
            "splunk.metrics.endpoint is deprecate, use otel.exporter.otlp.metrics.endpoint instead.");

        String otlpMetricsEndpoint = config.getString("otel.exporter.otlp.metrics.endpoint");
        String otlpEndpoint = config.getString("otel.exporter.otlp.endpoint");
        // If neither otel.exporter.otlp.metrics.endpoint nor otel.exporter.otlp.endpoint are
        // configured, the value of splunk.metrics.endpoint should be used as the exporter endpoint.
        if (otlpMetricsEndpoint == null && otlpEndpoint == null) {
          builder.addProperty("otel.exporter.otlp.metrics.endpoint", splunkMetricsEndpoint);
        }
      }

      String splunkMetricsInterval = config.getString(METRICS_EXPORT_INTERVAL_PROPERTY);
      if (splunkMetricsInterval != null) {
        log.warn(
            "splunk.metrics.export.interval is deprecate, use otel.metric.export.interval instead.");

        addIfAbsent(builder, config, "otel.metric.export.interval", splunkMetricsInterval);
      }
    } else {
      throw new IllegalStateException(
          "Invalid metrics implementation: '"
              + metricsImplementation
              + "', expected either 'micrometer' or 'opentelemetry'.");
    }

    String realm = config.getString(SPLUNK_REALM_PROPERTY, SPLUNK_REALM_NONE);
    if (SPLUNK_REALM_NONE.equals(realm)) {
      addIfAbsent(builder, config, OTEL_EXPORTER_JAEGER_ENDPOINT, "http://localhost:9080/v1/trace");
    } else {
      addIfAbsent(
          builder,
          config,
          "otel.exporter.otlp.endpoint",
          "https://ingest." + realm + ".signalfx.com");
      addIfAbsent(
          builder,
          config,
          OTEL_EXPORTER_JAEGER_ENDPOINT,
          "https://ingest." + realm + ".signalfx.com/v2/trace");

      // metrics ingest doesn't currently accept grpc
      addIfAbsent(builder, config, "otel.exporter.otlp.metrics.protocol", "http/protobuf");
      addIfAbsent(
          builder,
          config,
          "otel.exporter.otlp.metrics.endpoint",
          "https://ingest." + realm + ".signalfx.com/v2/datapoint/otlp");
    }

    String accessToken = config.getString(SPLUNK_ACCESS_TOKEN);
    if (accessToken != null) {
      String userOtlpHeaders = config.getString("otel.exporter.otlp.headers");
      String otlpHeaders =
          (userOtlpHeaders == null ? "" : userOtlpHeaders + ",") + "X-SF-TOKEN=" + accessToken;
      builder.addProperty("otel.exporter.otlp.headers", otlpHeaders);
    }

    return builder.build();
  }

  private static void addIfAbsent(ConfigBuilder builder, Config config, String key, String value) {
    if (config.getString(key) == null) {
      builder.addProperty(key, value);
    }
  }
}
