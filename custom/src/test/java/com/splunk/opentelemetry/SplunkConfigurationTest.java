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

import java.util.Map;
import org.junit.jupiter.api.Test;

class SplunkConfigurationTest {
  private static final String TEST_REALM = "test0";
  private static final String OTLP_ENDPOINT = "otel.exporter.otlp.traces.endpoint";

  @Test
  void usesRealmIngestUrlsIfRealmDefined() {
    assertRealmDefaults(configuration(TEST_REALM, null));
    assertRealmDefaults(configuration(null, TEST_REALM));
  }

  @Test
  void systemPropertyTakesPrecedence() {
    assertRealmDefaults(configuration("test1", TEST_REALM));
  }

  @Test
  void usesLocalIngestIfRealmIsNullOrNone() {
    assertLocalDefaults(configuration(null, null));
    assertLocalDefaults(configuration(null, SPLUNK_REALM_NONE));
    assertLocalDefaults(configuration(SPLUNK_REALM_NONE, null));
    assertLocalDefaults(configuration(SPLUNK_REALM_NONE, SPLUNK_REALM_NONE));
  }

  @Test
  void realmIsNotHardcoded() {
    var properties = configuration("test1", null);
    assertEquals(
        "https://ingest.test1.signalfx.com/v2/trace",
        properties.get(OTEL_EXPORTER_JAEGER_ENDPOINT));
    assertEquals("https://ingest.test1.signalfx.com", properties.get(OTLP_ENDPOINT));
  }

  private static void assertLocalDefaults(Map<String, String> properties) {
    assertEquals("http://localhost:9080/v1/trace", properties.get(OTEL_EXPORTER_JAEGER_ENDPOINT));
    assertNull(properties.get(OTLP_ENDPOINT));
  }

  private static void assertRealmDefaults(Map<String, String> properties) {
    assertEquals(
        "https://ingest.test0.signalfx.com/v2/trace",
        properties.get(OTEL_EXPORTER_JAEGER_ENDPOINT));
    assertEquals("https://ingest.test0.signalfx.com", properties.get(OTLP_ENDPOINT));
  }

  private static Map<String, String> configuration(String envValue, String propertyValue) {
    return new SplunkConfiguration(
            name -> SplunkConfiguration.SPLUNK_REALM_PROPERTY.equals(name) ? propertyValue : null,
            name -> "SPLUNK_REALM".equals(name) ? envValue : null)
        .defaultProperties();
  }
}
