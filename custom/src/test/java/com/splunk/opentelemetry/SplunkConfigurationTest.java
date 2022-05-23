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

import static com.splunk.opentelemetry.SplunkConfiguration.OTEL_EXPORTER_JAEGER_ENDPOINT;
import static com.splunk.opentelemetry.SplunkConfiguration.SPLUNK_REALM_NONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.opentelemetry.instrumentation.api.config.Config;
import org.junit.jupiter.api.Test;

class SplunkConfigurationTest {
  private static final String TEST_REALM = "test0";
  private static final String OTLP_ENDPOINT = "otel.exporter.otlp.traces.endpoint";

  @Test
  void usesRealmIngestUrlsIfRealmDefined() {
    assertRealmDefaults(configuration(TEST_REALM));
  }

  @Test
  void usesLocalIngestIfRealmIsNullOrNone() {
    assertLocalDefaults(configuration(null));
    assertLocalDefaults(configuration(SPLUNK_REALM_NONE));
  }

  @Test
  void realmIsNotHardcoded() {
    var config = configuration("test1");
    assertEquals(
        "https://ingest.test1.signalfx.com/v2/trace",
        config.getString(OTEL_EXPORTER_JAEGER_ENDPOINT));
    assertEquals("https://ingest.test1.signalfx.com", config.getString(OTLP_ENDPOINT));
  }

  @Test
  void shouldSetOtlpHeader() {
    SplunkConfiguration splunkConfiguration = new SplunkConfiguration();
    Config config =
        Config.builder()
            .addProperties(splunkConfiguration.defaultProperties())
            .addProperty(SplunkConfiguration.SPLUNK_ACCESS_TOKEN, "token")
            .build();

    config = splunkConfiguration.customize(config);

    assertEquals("X-SF-TOKEN=token", config.getString("otel.exporter.otlp.headers"));
  }

  @Test
  void shouldAppendToOtlpHeaders() {
    SplunkConfiguration splunkConfiguration = new SplunkConfiguration();
    Config config =
        Config.builder()
            .addProperties(splunkConfiguration.defaultProperties())
            .addProperty(SplunkConfiguration.SPLUNK_ACCESS_TOKEN, "token")
            .addProperty("otel.exporter.otlp.headers", "key=value")
            .build();

    config = splunkConfiguration.customize(config);

    assertEquals("key=value,X-SF-TOKEN=token", config.getString("otel.exporter.otlp.headers"));
  }

  private static void assertLocalDefaults(Config config) {
    assertEquals("http://localhost:9080/v1/trace", config.getString(OTEL_EXPORTER_JAEGER_ENDPOINT));
    assertNull(config.getString(OTLP_ENDPOINT));
  }

  private static void assertRealmDefaults(Config config) {
    assertEquals(
        "https://ingest.test0.signalfx.com/v2/trace",
        config.getString(OTEL_EXPORTER_JAEGER_ENDPOINT));
    assertEquals("https://ingest.test0.signalfx.com", config.getString(OTLP_ENDPOINT));
  }

  private static Config configuration(String realm) {
    SplunkConfiguration splunkConfiguration = new SplunkConfiguration();
    Config config =
        Config.builder()
            .addProperties(splunkConfiguration.defaultProperties())
            .addProperty(SplunkConfiguration.SPLUNK_REALM_PROPERTY, realm)
            .build();
    return splunkConfiguration.customize(config);
  }
}
