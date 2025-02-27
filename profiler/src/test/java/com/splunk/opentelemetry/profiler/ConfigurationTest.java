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

package com.splunk.opentelemetry.profiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConfigurationTest {

  String logsEndpoint = "http://logs.example.com";
  String otelEndpoint = "http://otel.example.com";

  @Test
  void getConfigUrl_endpointDefined() {
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(Configuration.CONFIG_KEY_OTEL_OTLP_URL, null)).thenReturn(otelEndpoint);
    when(config.getString(Configuration.CONFIG_KEY_INGEST_URL, otelEndpoint))
        .thenReturn(logsEndpoint);
    String result = Configuration.getConfigUrl(config);
    assertEquals(logsEndpoint, result);
  }

  @Test
  void getConfigUrl_endpointNotDefined() {
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(Configuration.CONFIG_KEY_OTEL_OTLP_URL, null)).thenReturn(otelEndpoint);
    when(config.getString(Configuration.CONFIG_KEY_INGEST_URL, otelEndpoint))
        .thenReturn(otelEndpoint);
    String result = Configuration.getConfigUrl(config);
    assertEquals(otelEndpoint, result);
  }

  @Test
  void getConfigUrlNull() {
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(Configuration.CONFIG_KEY_OTEL_OTLP_URL, null)).thenReturn(null);
    when(config.getString(Configuration.CONFIG_KEY_INGEST_URL, null)).thenReturn(null);
    String result = Configuration.getConfigUrl(config);
    assertNull(result);
  }

  @Test
  void getConfigUrlSplunkRealm() {
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getString(Configuration.CONFIG_KEY_OTEL_OTLP_URL, null))
        .thenReturn("https://ingest.us0.signalfx.com");
    when(config.getString(Configuration.CONFIG_KEY_INGEST_URL, null)).thenReturn(null);
    String result = Configuration.getConfigUrl(config);
    assertNull(result);
  }

  @Test
  void getOtlpProtocolDefault() {
    String result =
        Configuration.getOtlpProtocol(DefaultConfigProperties.create(Collections.emptyMap()));
    assertEquals("http/protobuf", result);
  }

  @Test
  void getOtlpProtocolOtelPropertySet() {
    String result =
        Configuration.getOtlpProtocol(
            DefaultConfigProperties.create(
                Collections.singletonMap("otel.exporter.otlp.protocol", "test")));
    assertEquals("test", result);
  }

  @Test
  void getOtlpProtocol() {
    Map<String, String> map = new HashMap<>();
    map.put("otel.exporter.otlp.protocol", "test1");
    map.put("splunk.profiler.otlp.protocol", "test2");
    String result = Configuration.getOtlpProtocol(DefaultConfigProperties.create(map));
    assertEquals("test2", result);
  }

  @Test
  void snapshotProfilingDisabledByDefault() {
    Configuration configuration = new Configuration();
    var properties = configuration.defaultProperties();
    assertEquals("false", properties.get("splunk.snapshot.profiler.enabled"));
  }

  @Test
  void snapshotSelectionDefaultRate() {
    Configuration configuration = new Configuration();
    var properties = configuration.defaultProperties();

    double rate = Double.parseDouble(properties.get("splunk.snapshot.selection.rate"));
    assertEquals(0.01, rate);
  }

  @ParameterizedTest
  @ValueSource(doubles = {1.0, 0.5, 0.0})
  void getSnapshotSelectionRate(double selectionRate) {
    var properties =
        DefaultConfigProperties.create(
            Map.of("splunk.snapshot.selection.rate", String.valueOf(selectionRate)));

    double actualSelectionRate = Configuration.getSnapshotSelectionRate(properties);
    assertEquals(selectionRate, actualSelectionRate);
  }

  @Test
  void getSnapshotSelectionRateUsesDefaultWhenPropertyNotSet() {
    var properties = DefaultConfigProperties.create(Collections.emptyMap());

    double actualSelectionRate = Configuration.getSnapshotSelectionRate(properties);
    assertEquals(0.01, actualSelectionRate);
  }
}
