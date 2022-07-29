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

import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.instrumentation.api.config.ConfigBuilder;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class SplunkConfigurationTest {

  private static final String OTLP_ENDPOINT = "otel.exporter.otlp.endpoint";

  @Test
  void usesLocalIngestIfNoRealmIsConfigured() {
    Config config = configuration();

    assertEquals("http://localhost:9080/v1/trace", config.getString(OTEL_EXPORTER_JAEGER_ENDPOINT));
    assertNull(config.getString(OTLP_ENDPOINT));
  }

  @Test
  void usesLocalIngestIfRealmIsNone() {
    Config config =
        configuration(
            builder ->
                builder.addProperty(SplunkConfiguration.SPLUNK_REALM_PROPERTY, SPLUNK_REALM_NONE));

    assertEquals("http://localhost:9080/v1/trace", config.getString(OTEL_EXPORTER_JAEGER_ENDPOINT));
    assertNull(config.getString(OTLP_ENDPOINT));
  }

  @Test
  void realmIsNotHardcoded() {
    var config =
        configuration(
            builder -> builder.addProperty(SplunkConfiguration.SPLUNK_REALM_PROPERTY, "test1"));

    assertEquals(
        "https://ingest.test1.signalfx.com/v2/trace",
        config.getString(OTEL_EXPORTER_JAEGER_ENDPOINT));
    assertEquals("https://ingest.test1.signalfx.com", config.getString(OTLP_ENDPOINT));
  }

  @Test
  void shouldSetOtlpHeader() {
    Config config =
        configuration(
            builder -> builder.addProperty(SplunkConfiguration.SPLUNK_ACCESS_TOKEN, "token"));

    assertEquals("X-SF-TOKEN=token", config.getString("otel.exporter.otlp.headers"));
  }

  @Test
  void shouldAppendToOtlpHeaders() {
    Config config =
        configuration(
            builder ->
                builder
                    .addProperty(SplunkConfiguration.SPLUNK_ACCESS_TOKEN, "token")
                    .addProperty("otel.exporter.otlp.headers", "key=value"));

    assertEquals("key=value,X-SF-TOKEN=token", config.getString("otel.exporter.otlp.headers"));
  }

  @Test
  void memoryProfilerEnablesMetrics() {
    Config config =
        configuration(builder -> builder.addProperty(PROFILER_MEMORY_ENABLED_PROPERTY, "true"));

    assertTrue(config.getBoolean(METRICS_ENABLED_PROPERTY, false));
  }

  @Test
  void shouldDisableMetricsByDefault() {
    Config config = configuration();

    assertFalse(config.getBoolean(METRICS_ENABLED_PROPERTY, false));
    assertEquals("none", config.getString("otel.metrics.exporter"));

    verifyThatOtelMetricsInstrumentationsAreDisabled(config);
  }

  @Test
  void shouldChooseMicrometerAsTheDefaultMetricsImplementation() {
    Config config = configuration(builder -> builder.addProperty(METRICS_ENABLED_PROPERTY, "true"));

    assertTrue(config.getBoolean(METRICS_ENABLED_PROPERTY, false));
    assertEquals("micrometer", config.getString(METRICS_IMPLEMENTATION));
    assertEquals("none", config.getString("otel.metrics.exporter"));

    verifyThatOtelMetricsInstrumentationsAreDisabled(config);
  }

  @Test
  void shouldDisableMicrometerMetricsIfOtelImplementationIsChosen() {
    Config config =
        configuration(
            builder ->
                builder
                    .addProperty(METRICS_ENABLED_PROPERTY, "true")
                    .addProperty(METRICS_IMPLEMENTATION, "opentelemetry"));

    assertTrue(config.getBoolean(METRICS_ENABLED_PROPERTY, false));
    assertEquals("opentelemetry", config.getString(METRICS_IMPLEMENTATION));
    assertNotEquals("none", config.getString("otel.metrics.exporter"));

    verifyThatMicrometerInstrumentationsAreDisabled(config);
  }

  private static Config configuration() {
    return configuration(b -> {});
  }

  private static Config configuration(Consumer<ConfigBuilder> customizer) {
    SplunkConfiguration splunkConfiguration = new SplunkConfiguration();
    ConfigBuilder configBuilder =
        Config.builder().addProperties(splunkConfiguration.defaultProperties());
    customizer.accept(configBuilder);
    return splunkConfiguration.customize(configBuilder.build());
  }

  private static void verifyThatOtelMetricsInstrumentationsAreDisabled(Config config) {
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

  private static void verifyThatMicrometerInstrumentationsAreDisabled(Config config) {
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

  private static void assertInstrumentationDisabled(Config config, String name) {
    assertFalse(config.getBoolean("otel.instrumentation." + name + ".enabled", false));
  }
}
