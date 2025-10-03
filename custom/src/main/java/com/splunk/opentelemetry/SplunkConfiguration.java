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

import static java.util.logging.Level.WARNING;

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
  public static final String PROFILER_ENABLED_PROPERTY = "splunk.profiler.enabled";
  public static final String PROFILER_MEMORY_ENABLED_PROPERTY = "splunk.profiler.memory.enabled";
  public static final String SPLUNK_REALM_PROPERTY = "splunk.realm";
  public static final String SPLUNK_REALM_NONE = "none";

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

    // truncate commandline when metrics enabled by default
    config.put(METRICS_FULL_COMMAND_LINE, "false");

    // enable spring batch instrumentation
    config.put("otel.instrumentation.spring-batch.enabled", "true");
    config.put("otel.instrumentation.spring-batch.item.enabled", "true");

    return config;
  }

  Map<String, String> customize(ConfigProperties config) {
    Map<String, String> customized = new HashMap<>();

    String realm = config.getString(SPLUNK_REALM_PROPERTY, SPLUNK_REALM_NONE);
    if (!SPLUNK_REALM_NONE.equals(realm)) {
      addIfAbsent(
          customized,
          config,
          "otel.exporter.otlp.endpoint",
          RealmUrls.INSTANCE.otlpEndpoint(realm));

      // metrics ingest doesn't currently accept grpc
      addIfAbsent(customized, config, "otel.exporter.otlp.metrics.protocol", "http/protobuf");
      addIfAbsent(
          customized,
          config,
          "otel.exporter.otlp.metrics.endpoint",
          RealmUrls.INSTANCE.otlpMetrics(realm));

      if (config.getString("otel.exporter.otlp.logs.endpoint") == null) {
        String logsEndpoint = getDefaultLogsEndpoint(config);
        logger.log(
            WARNING,
            "Logs can not be sent to {0}, using {1} instead. "
                + "You can override it by setting otel.exporter.otlp.logs.endpoint",
            new Object[] {RealmUrls.INSTANCE.otlpLogs(realm), logsEndpoint});

        addIfAbsent(customized, config, "otel.exporter.otlp.logs.endpoint", logsEndpoint);
      }
    }

    String accessToken = config.getString(SPLUNK_ACCESS_TOKEN);
    if (accessToken != null) {
      String userOtlpHeaders = config.getString("otel.exporter.otlp.headers");
      String otlpHeaders =
          (userOtlpHeaders == null ? "" : userOtlpHeaders + ",") + "X-SF-TOKEN=" + accessToken;
      customized.put("otel.exporter.otlp.headers", otlpHeaders);
    }

    // disable logging instrumentations if the logging exporter is not used
    if ("none".equals(config.getString("otel.logs.exporter"))) {
      customized.put("otel.instrumentation.java-util-logging.enabled", "false");
      customized.put("otel.instrumentation.jboss-logmanager.enabled", "false");
      customized.put("otel.instrumentation.log4j-appender.enabled", "false");
      customized.put("otel.instrumentation.logback-appender.enabled", "false");
    }

    EndpointProtocolValidator.validate(customized, config, logger::warning);

    return customized;
  }

  private static String getDefaultLogsEndpoint(ConfigProperties config) {
    return "http/protobuf".equals(getOtlpLogsProtocol(config))
        ? "http://localhost:4318/v1/logs"
        : "http://localhost:4317";
  }

  public static String getOtlpLogsProtocol(ConfigProperties config) {
    return config.getString(
        "otel.exporter.otlp.logs.protocol", config.getString("otel.exporter.otlp.protocol"));
  }

  private static void addIfAbsent(
      Map<String, String> customized, ConfigProperties config, String key, String value) {
    if (config.getString(key) == null) {
      customized.put(key, value);
    }
  }
}
