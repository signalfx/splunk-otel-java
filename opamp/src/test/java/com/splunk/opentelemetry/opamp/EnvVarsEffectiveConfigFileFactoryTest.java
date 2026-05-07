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

package com.splunk.opentelemetry.opamp;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class EnvVarsEffectiveConfigFileFactoryTest {

  @Test
  void buildFileContent_reportsConfiguredValues() throws IOException {
    Properties fileContent =
        loadProperties(
            Map.of(
                "splunk.profiler.enabled", "true",
                "splunk.profiler.memory.enabled", "true",
                "splunk.snapshot.profiler.enabled", "true",
                "splunk.snapshot.sampling.interval", "26ms",
                "splunk.profiler.call.stack.interval", "1235ms",
                "otel.exporter.otlp.endpoint", "https://base.example.com",
                "otel.exporter.otlp.traces.endpoint", "https://traces.example.com",
                "otel.exporter.otlp.metrics.endpoint", "https://metrics.example.com",
                "otel.exporter.otlp.logs.endpoint", "https://logs.example.com",
                "otel.service.name", "checkout"));

    assertProperties(
        fileContent,
        Map.of(
            "SPLUNK_PROFILER_ENABLED", "true",
            "SPLUNK_PROFILER_MEMORY_ENABLED", "true",
            "SPLUNK_SNAPSHOT_PROFILER_ENABLED", "true",
            "SPLUNK_SNAPSHOT_PROFILER_SAMPLING_INTERVAL", "\"26ms\"",
            "SPLUNK_PROFILER_CALL_STACK_INTERVAL", "\"1235ms\"",
            "OTEL_EXPORTER_OTLP_TRACES_ENDPOINTS", "\"https://traces.example.com\"",
            "OTEL_EXPORTER_OTLP_METRICS_ENDPOINTS", "\"https://metrics.example.com\"",
            "OTEL_EXPORTER_OTLP_LOGS_ENDPOINTS", "\"https://logs.example.com\""));
    assertThat(fileContent.size()).isEqualTo(8);
  }

  @Test
  void buildFileContent_reportsDefaultValuesWhenNotConfigured() throws IOException {
    Properties fileContent = loadProperties(Map.of());

    assertProperties(
        fileContent,
        Map.of(
            "SPLUNK_PROFILER_ENABLED", "false",
            "SPLUNK_PROFILER_MEMORY_ENABLED", "false",
            "SPLUNK_SNAPSHOT_PROFILER_ENABLED", "false",
            "SPLUNK_SNAPSHOT_PROFILER_SAMPLING_INTERVAL", "\"10ms\"",
            "SPLUNK_PROFILER_CALL_STACK_INTERVAL", "\"10000ms\"",
            "OTEL_EXPORTER_OTLP_TRACES_ENDPOINTS", "\"http://localhost:4318/v1/traces\"",
            "OTEL_EXPORTER_OTLP_METRICS_ENDPOINTS", "\"http://localhost:4318/v1/metrics\"",
            "OTEL_EXPORTER_OTLP_LOGS_ENDPOINTS", "\"http://localhost:4318/v1/logs\""));
    assertThat(fileContent.size()).isEqualTo(8);
  }

  @Test
  void buildFileContent_appendsSignalPathsToBaseHttpProtobufEndpoint() throws IOException {
    Properties fileContent =
        loadProperties(Map.of("otel.exporter.otlp.endpoint", "https://collector:4318"));

    assertProperties(
        fileContent,
        Map.of(
            "OTEL_EXPORTER_OTLP_TRACES_ENDPOINTS", "\"https://collector:4318/v1/traces\"",
            "OTEL_EXPORTER_OTLP_METRICS_ENDPOINTS", "\"https://collector:4318/v1/metrics\"",
            "OTEL_EXPORTER_OTLP_LOGS_ENDPOINTS", "\"https://collector:4318/v1/logs\""));
  }

  @Test
  void buildFileContent_usesBaseGrpcEndpointForAllSignals() throws IOException {
    Properties fileContent =
        loadProperties(
            Map.of(
                "otel.exporter.otlp.endpoint", "https://collector:4317",
                "otel.exporter.otlp.protocol", "grpc"));

    assertProperties(
        fileContent,
        Map.of(
            "OTEL_EXPORTER_OTLP_TRACES_ENDPOINTS", "\"https://collector:4317\"",
            "OTEL_EXPORTER_OTLP_METRICS_ENDPOINTS", "\"https://collector:4317\"",
            "OTEL_EXPORTER_OTLP_LOGS_ENDPOINTS", "\"https://collector:4317\""));
  }

  @Test
  void buildFileContent_usesSignalSpecificProtocolWhenResolvingEndpoints() throws IOException {
    Properties fileContent =
        loadProperties(
            Map.of(
                "otel.exporter.otlp.endpoint", "https://collector:4317",
                "otel.exporter.otlp.traces.protocol", "grpc",
                "otel.exporter.otlp.metrics.protocol", "grpc",
                "otel.exporter.otlp.logs.protocol", "grpc"));

    assertProperties(
        fileContent,
        Map.of(
            "OTEL_EXPORTER_OTLP_TRACES_ENDPOINTS", "\"https://collector:4317\"",
            "OTEL_EXPORTER_OTLP_METRICS_ENDPOINTS", "\"https://collector:4317\"",
            "OTEL_EXPORTER_OTLP_LOGS_ENDPOINTS", "\"https://collector:4317\""));
  }

  private static String createFileContent(Map<String, String> configMap) {
    DefaultConfigProperties config = DefaultConfigProperties.createFromMap(configMap);
    return new EnvVarsEffectiveConfigFileFactory(config).buildFileContent();
  }

  private static Properties loadProperties(Map<String, String> configMap) throws IOException {
    Properties properties = new Properties();
    properties.load(new StringReader(createFileContent(configMap)));
    return properties;
  }

  private static void assertProperties(Properties fileContent, Map<String, String> expectedValues) {
    expectedValues.forEach(
        (propertyName, expectedValue) -> assertProperty(fileContent, propertyName, expectedValue));
  }

  private static void assertProperty(
      Properties fileContent, String propertyName, String expectedValue) {
    assertThat(fileContent.getProperty(propertyName)).isEqualTo(expectedValue);
  }
}
