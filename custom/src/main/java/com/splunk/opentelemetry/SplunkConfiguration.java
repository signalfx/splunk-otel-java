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

import static java.util.Arrays.asList;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class SplunkConfiguration implements AutoConfigurationCustomizerProvider {
  private static final Logger logger = Logger.getLogger(SplunkConfiguration.class.getName());

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
  // used for testing micrometer metrics
  public static final String METRICS_MICROMETER_DISABLED = "splunk.metrics.micrometer.disabled";
  public static final String METRICS_FULL_COMMAND_LINE = "splunk.metrics.force_full_commandline";

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration
        .addPropertiesSupplier(this::defaultProperties)
        .addPropertiesCustomizer(this::customize);
  }

  Map<String, String> defaultProperties() {
    Map<String, String> config = new HashMap<>();

    config.put("otel.traces.sampler", "always_on");

    // by default metrics are disabled
    config.put(METRICS_ENABLED_PROPERTY, "false");
    // micrometer is the default implementation
    config.put(METRICS_IMPLEMENTATION, "micrometer");
    // truncate commandline when metrics enabled by default
    config.put(METRICS_FULL_COMMAND_LINE, "false");

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

  Map<String, String> customize(ConfigProperties config) {
    Map<String, String> customized = new HashMap<>();

    boolean memoryProfilerEnabled = config.getBoolean(PROFILER_MEMORY_ENABLED_PROPERTY, false);
    // memory profiler implies metrics
    if (memoryProfilerEnabled) {
      customized.put(METRICS_ENABLED_PROPERTY, "true");
    }

    boolean metricsEnabled = config.getBoolean(METRICS_ENABLED_PROPERTY, memoryProfilerEnabled);
    String metricsImplementation = config.getString(METRICS_IMPLEMENTATION);

    if (!metricsEnabled || "micrometer".equals(metricsImplementation)) {
      // by default no otel metrics are exported
      addIfAbsent(customized, config, "otel.metrics.exporter", "none");

      // disable upstream metrics instrumentations
      // metrics are disabled, or we're still on the micrometer based implementation
      for (String otelInstrumentationName :
          asList(
              "apache-dbcp",
              "c3p0",
              "hikaricp",
              "micrometer",
              "oracle-ucp",
              "oshi",
              "spring-boot-actuator-autoconfigure",
              "runtime-metrics",
              "tomcat-jdbc",
              "vibur-dbcp")) {
        disableInstrumentation(customized, config, otelInstrumentationName);
      }
      // disable Kafka metric reporter
      addIfAbsent(
          customized, config, "otel.instrumentation.kafka.metric-reporter.enabled", "false");
    }
    if ("micrometer".equals(metricsImplementation)) {
      // TODO: warn that micrometer metrics are deprecated
      // log.info(
      //     "Micrometer metrics are deprecated, OpenTelemetry metrics will become the default
      // implementation in the next release.");
    } else if ("opentelemetry".equals(metricsImplementation)) {
      String splunkMetricsEndpoint = config.getString(METRICS_ENDPOINT_PROPERTY);
      if (splunkMetricsEndpoint != null) {
        logger.warning(
            "splunk.metrics.endpoint is deprecate, use otel.exporter.otlp.metrics.endpoint instead.");

        String otlpMetricsEndpoint = config.getString("otel.exporter.otlp.metrics.endpoint");
        String otlpEndpoint = config.getString("otel.exporter.otlp.endpoint");
        // If neither otel.exporter.otlp.metrics.endpoint nor otel.exporter.otlp.endpoint are
        // configured, the value of splunk.metrics.endpoint should be used as the exporter endpoint.
        if (otlpMetricsEndpoint == null && otlpEndpoint == null) {
          customized.put("otel.exporter.otlp.metrics.endpoint", splunkMetricsEndpoint);
        }
      }

      String splunkMetricsInterval = config.getString(METRICS_EXPORT_INTERVAL_PROPERTY);
      if (splunkMetricsInterval != null) {
        logger.warning(
            "splunk.metrics.export.interval is deprecate, use otel.metric.export.interval instead.");

        addIfAbsent(customized, config, "otel.metric.export.interval", splunkMetricsInterval);
      }

      // disable micrometer metrics, we'll be using the equivalent otel metrics from the upstream
      // agent
      for (String micrometerInstrumentationName :
          asList(
              "c3p0-splunk",
              "commons-dbcp2-splunk",
              "hikari-splunk",
              "micrometer-splunk",
              "oracle-ucp-splunk",
              "tomcat-jdbc-splunk",
              "vibur-dbcp-splunk")) {
        disableInstrumentation(customized, config, micrometerInstrumentationName);
      }
    } else {
      throw new IllegalStateException(
          "Invalid metrics implementation: '"
              + metricsImplementation
              + "', expected either 'micrometer' or 'opentelemetry'.");
    }

    String realm = config.getString(SPLUNK_REALM_PROPERTY, SPLUNK_REALM_NONE);
    if (SPLUNK_REALM_NONE.equals(realm)) {
      addIfAbsent(
          customized, config, OTEL_EXPORTER_JAEGER_ENDPOINT, "http://localhost:9080/v1/trace");
    } else {
      addIfAbsent(
          customized,
          config,
          "otel.exporter.otlp.endpoint",
          "https://ingest." + realm + ".signalfx.com");
      addIfAbsent(
          customized,
          config,
          OTEL_EXPORTER_JAEGER_ENDPOINT,
          "https://ingest." + realm + ".signalfx.com/v2/trace");

      // metrics ingest doesn't currently accept grpc
      addIfAbsent(customized, config, "otel.exporter.otlp.metrics.protocol", "http/protobuf");
      addIfAbsent(
          customized,
          config,
          "otel.exporter.otlp.metrics.endpoint",
          "https://ingest." + realm + ".signalfx.com/v2/datapoint/otlp");
    }

    String accessToken = config.getString(SPLUNK_ACCESS_TOKEN);
    if (accessToken != null) {
      String userOtlpHeaders = config.getString("otel.exporter.otlp.headers");
      String otlpHeaders =
          (userOtlpHeaders == null ? "" : userOtlpHeaders + ",") + "X-SF-TOKEN=" + accessToken;
      customized.put("otel.exporter.otlp.headers", otlpHeaders);
    }

    return customized;
  }

  private static void disableInstrumentation(
      Map<String, String> customized, ConfigProperties config, String instrumentationName) {
    addIfAbsent(
        customized, config, "otel.instrumentation." + instrumentationName + ".enabled", "false");
  }

  private static void addIfAbsent(
      Map<String, String> customized, ConfigProperties config, String key, String value) {
    if (config.getString(key) == null) {
      customized.put(key, value);
    }
  }
}
