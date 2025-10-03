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

import static com.splunk.opentelemetry.SplunkConfiguration.SPLUNK_REALM_NONE;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getConfig;
import static org.assertj.core.api.Assertions.assertThat;

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

    assertThat(config.getString(OTLP_ENDPOINT)).isNull();
  }

  @Test
  void usesLocalIngestIfRealmIsNone() {
    ConfigProperties config =
        configuration(() -> Map.of(SplunkConfiguration.SPLUNK_REALM_PROPERTY, SPLUNK_REALM_NONE));

    assertThat(config.getString(OTLP_ENDPOINT)).isNull();
  }

  @Test
  void realmIsNotHardcoded() {
    var config = configuration(() -> Map.of(SplunkConfiguration.SPLUNK_REALM_PROPERTY, "test1"));
    var expected = RealmUrls.INSTANCE.otlpEndpoint("test1");
    assertThat(config.getString(OTLP_ENDPOINT)).isEqualTo(expected);
  }

  @Test
  void shouldSetOtlpHeader() {
    ConfigProperties config =
        configuration(() -> Map.of(SplunkConfiguration.SPLUNK_ACCESS_TOKEN, "token"));

    assertThat(config.getString("otel.exporter.otlp.headers")).isEqualTo("X-SF-TOKEN=token");
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

    assertThat(config.getString("otel.exporter.otlp.headers"))
        .isEqualTo("key=value,X-SF-TOKEN=token");
  }

  @Test
  void shouldDisableLoggingInstrumentationsWhenExporterIsNone() {
    ConfigProperties config = configuration(() -> Map.of("otel.logs.exporter", "none"));

    assertThat(config.getString("otel.logs.exporter")).isEqualTo("none");
    assertInstrumentationDisabled(config, "java-util-logging");
    assertInstrumentationDisabled(config, "jboss-logmanager");
    assertInstrumentationDisabled(config, "log4j-appender");
    assertInstrumentationDisabled(config, "logback-appender");
  }

  @Test
  void shouldNotDisableLoggingInstrumentationsWhenExporterIsSet() {
    ConfigProperties config = configuration(() -> Map.of("otel.logs.exporter", "otlp"));

    assertThat(config.getString("otel.logs.exporter")).isEqualTo("otlp");
    assertInstrumentationNotChanged(config, "java-util-logging");
    assertInstrumentationNotChanged(config, "jboss-logmanager");
    assertInstrumentationNotChanged(config, "log4j-appender");
    assertInstrumentationNotChanged(config, "logback-appender");
  }

  private static ConfigProperties configuration() {
    return configuration(Map::of);
  }

  static ConfigProperties configuration(Supplier<Map<String, String>> testPropertiesSupplier) {
    AutoConfiguredOpenTelemetrySdk sdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            // don't create the SDK
            .addPropertiesSupplier(() -> Map.of("otel.experimental.sdk.enabled", "false"))
            // run in a customizer so that it executes after SplunkConfiguration#defaultProperties()
            .addPropertiesCustomizer(config -> testPropertiesSupplier.get())
            // implicitly loads SplunkConfiguration through SPI
            .build();
    return getConfig(sdk);
  }

  private static void assertInstrumentationDisabled(ConfigProperties config, String name) {
    assertThat(config.getBoolean("otel.instrumentation." + name + ".enabled"))
        .as("Instrumentation %s is turned off", name)
        .isFalse();
  }

  private static void assertInstrumentationNotChanged(ConfigProperties config, String name) {
    assertThat(config.getBoolean("otel.instrumentation." + name + ".enabled"))
        .as("Instrumentation %s is not changed", name)
        .isNull();
  }
}
