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

import static com.splunk.opentelemetry.SplunkConfiguration.METRICS_ENABLED_PROPERTY;
import static com.splunk.opentelemetry.SplunkConfiguration.METRICS_IMPLEMENTATION;
import static com.splunk.opentelemetry.SplunkConfiguration.OTEL_EXPORTER_JAEGER_ENDPOINT;
import static com.splunk.opentelemetry.SplunkConfiguration.PROFILER_MEMORY_ENABLED_PROPERTY;
import static com.splunk.opentelemetry.SplunkConfiguration.SPLUNK_REALM_NONE;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class SplunkConfigurationTest {

  private static final String OTLP_ENDPOINT = "otel.exporter.otlp.endpoint";

  @Test
  void usesLocalIngestIfNoRealmIsConfigured() {
    ConfigProperties config = configuration();

    assertEquals("http://localhost:9080/v1/trace", config.getString(OTEL_EXPORTER_JAEGER_ENDPOINT));
    assertNull(config.getString(OTLP_ENDPOINT));
  }

  @Test
  void usesLocalIngestIfRealmIsNone() {
    ConfigProperties config =
        configuration(() -> Map.of(SplunkConfiguration.SPLUNK_REALM_PROPERTY, SPLUNK_REALM_NONE));

    assertEquals("http://localhost:9080/v1/trace", config.getString(OTEL_EXPORTER_JAEGER_ENDPOINT));
    assertNull(config.getString(OTLP_ENDPOINT));
  }

  @Test
  void realmIsNotHardcoded() {
    var config = configuration(() -> Map.of(SplunkConfiguration.SPLUNK_REALM_PROPERTY, "test1"));

    assertEquals(
        "https://ingest.test1.signalfx.com/v2/trace",
        config.getString(OTEL_EXPORTER_JAEGER_ENDPOINT));
    assertEquals("https://ingest.test1.signalfx.com", config.getString(OTLP_ENDPOINT));
  }

  @Test
  void shouldSetOtlpHeader() {
    ConfigProperties config =
        configuration(() -> Map.of(SplunkConfiguration.SPLUNK_ACCESS_TOKEN, "token"));

    assertEquals("X-SF-TOKEN=token", config.getString("otel.exporter.otlp.headers"));
  }

  @Test
  void shouldAppendToOtlpHeaders() {
    ConfigProperties config =
        configuration(
            () ->
                Map.of(
                    SplunkConfiguration.SPLUNK_ACCESS_TOKEN,
                    "token",
                    "otel.exporter.otlp.headers",
                    "key=value"));

    assertEquals("key=value,X-SF-TOKEN=token", config.getString("otel.exporter.otlp.headers"));
  }

  @Test
  void memoryProfilerEnablesMetrics() {
    ConfigProperties config = configuration(() -> Map.of(PROFILER_MEMORY_ENABLED_PROPERTY, "true"));

    assertTrue(config.getBoolean(METRICS_ENABLED_PROPERTY, false));
  }

  @Test
  void shouldDisableMetricsByDefault() {
    ConfigProperties config = configuration();

    assertFalse(config.getBoolean(METRICS_ENABLED_PROPERTY, false));
    assertEquals("none", config.getString("otel.metrics.exporter"));

    verifyThatOtelMetricsInstrumentationsAreDisabled(config);
  }

  @Test
  void shouldChooseMicrometerAsTheDefaultMetricsImplementation() {
    ConfigProperties config = configuration(() -> Map.of(METRICS_ENABLED_PROPERTY, "true"));

    assertTrue(config.getBoolean(METRICS_ENABLED_PROPERTY, false));
    assertEquals("micrometer", config.getString(METRICS_IMPLEMENTATION));
    assertEquals("none", config.getString("otel.metrics.exporter"));

    verifyThatOtelMetricsInstrumentationsAreDisabled(config);
  }

  @Test
  void shouldDisableMicrometerMetricsIfOtelImplementationIsChosen() {
    ConfigProperties config =
        configuration(
            () ->
                Map.of(METRICS_ENABLED_PROPERTY, "true", METRICS_IMPLEMENTATION, "opentelemetry"));

    assertTrue(config.getBoolean(METRICS_ENABLED_PROPERTY, false));
    assertEquals("opentelemetry", config.getString(METRICS_IMPLEMENTATION));
    assertNotEquals("none", config.getString("otel.metrics.exporter"));

    verifyThatMicrometerInstrumentationsAreDisabled(config);
  }

  private static ConfigProperties configuration() {
    return configuration(Map::of);
  }

  private static ConfigProperties configuration(
      Supplier<Map<String, String>> testPropertiesSupplier) {
    return AutoConfiguredOpenTelemetrySdk.builder()
        .setResultAsGlobal(false)
        // don't create the SDK
        .addPropertiesSupplier(() -> Map.of("otel.experimental.sdk.enabled", "false"))
        // run in a customizer so that it executes after SplunkConfiguration#defaultProperties()
        .addPropertiesCustomizer(config -> testPropertiesSupplier.get())
        // implicitly loads SplunkConfiguration through SPI
        .build()
        .getConfig();
  }

  private static void verifyThatOtelMetricsInstrumentationsAreDisabled(ConfigProperties config) {
    for (String instrumentationName :
        asList(
            "apache-dbcp",
            "c3p0",
            "hikaricp",
            "micrometer",
            "oracle-ucp",
            "oshi",
            "runtime-metrics",
            "spring-boot-autoconfigure",
            "tomcat-jdbc",
            "vibur-dbcp")) {
      assertInstrumentationDisabled(config, instrumentationName);
    }
  }

  private static void verifyThatMicrometerInstrumentationsAreDisabled(ConfigProperties config) {
    for (String instrumentationName :
        asList(
            "c3p0-splunk",
            "commons-dbcp2-splunk",
            "hikari-splunk",
            "micrometer-splunk",
            "oracle-ucp-splunk",
            "tomcat-jdbc-splunk",
            "vibut-dbcp-splunk")) {
      assertInstrumentationDisabled(config, instrumentationName);
    }
  }

  private static void assertInstrumentationDisabled(ConfigProperties config, String name) {
    assertFalse(config.getBoolean("otel.instrumentation." + name + ".enabled", false));
  }
}
