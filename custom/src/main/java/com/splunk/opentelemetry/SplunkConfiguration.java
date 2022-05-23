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

@AutoService(ConfigCustomizer.class)
public class SplunkConfiguration implements ConfigCustomizer {
  public static final String SPLUNK_ACCESS_TOKEN = "splunk.access.token";
  public static final String OTEL_EXPORTER_JAEGER_ENDPOINT = "otel.exporter.jaeger.endpoint";
  public static final String PROFILER_ENABLED_PROPERTY = "splunk.profiler.enabled";
  public static final String PROFILER_MEMORY_ENABLED_PROPERTY = "splunk.profiler.memory.enabled";
  public static final String SPLUNK_REALM_PROPERTY = "splunk.realm";
  public static final String SPLUNK_REALM_NONE = "none";

  @Override
  public Map<String, String> defaultProperties() {
    Map<String, String> config = new HashMap<>();

    config.put("otel.traces.sampler", "always_on");

    // by default no metrics are exported
    config.put("otel.metrics.exporter", "none");

    // instrumentation settings

    // disable logging instrumentations - we're not currently sending logs (yet)
    config.put("otel.instrumentation.java-util-logging.enabled", "false");
    config.put("otel.instrumentation.jboss-logmanager.enabled", "false");
    config.put("otel.instrumentation.log4j-appender.enabled", "false");
    config.put("otel.instrumentation.logback-appender.enabled", "false");
    // disable otel hikari instrumentation, we use our own for now
    config.put("otel.instrumentation.hikaricp.enabled", "false");
    // disable otel micrometer instrumentation, we use our own for now
    config.put("otel.instrumentation.micrometer.enabled", "false");
    // disable oshi metrics too, just in case
    config.put("otel.instrumentation.oshi.enabled", "false");
    // disable otel runtime-metrics instrumentation; we use micrometer metrics instead
    config.put("otel.instrumentation.runtime-metrics.enabled", "false");
    // enable spring batch instrumentation
    config.put("otel.instrumentation.spring-batch.enabled", "true");
    config.put("otel.instrumentation.spring-batch.item.enabled", "true");

    return config;
  }

  @Override
  public Config customize(Config config) {
    ConfigBuilder builder = config.toBuilder();

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
