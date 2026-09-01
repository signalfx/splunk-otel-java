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

package com.splunk.opentelemetry.opamp.effectiveconfig;

import static org.assertj.core.api.Assertions.assertThat;

import com.splunk.opentelemetry.profiler.ProfilerConfiguration;
import com.splunk.opentelemetry.profiler.ProfilerEnvVarsConfigurationFactory;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingConfiguration;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingEnvVarsConfigurationFactory;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EnvVarsEffectiveConfigFileFactoryTest {

  @AfterEach
  void tearDown() {
    ProfilerConfiguration.SUPPLIER.reset();
    SnapshotProfilingConfiguration.SUPPLIER.reset();
  }

  @Test
  void createFile_reportsCorrectContentType() {
    DefaultConfigProperties config = DefaultConfigProperties.createFromMap(Map.ofEntries());
    String contentType = new EnvVarsEffectiveConfigFileFactory(config).getContentType();

    assertThat(contentType).isEqualTo("text/plain; format=properties; vendor=splunk; v=1.0.0");
  }

  @Test
  void buildFileContent_reportsConfiguredValues() throws IOException {
    Properties fileContent =
        createFileContent(
            Map.ofEntries(
                Map.entry("splunk.profiler.enabled", "true"),
                Map.entry("splunk.profiler.memory.enabled", "true"),
                Map.entry("splunk.snapshot.profiler.enabled", "true"),
                Map.entry("splunk.snapshot.sampling.interval", "26ms"),
                Map.entry("splunk.snapshot.selection.probability", "0.0123"),
                Map.entry("splunk.profiler.call.stack.interval", "1235ms"),
                Map.entry("otel.exporter.otlp.endpoint", "https://base.example.com"),
                Map.entry("otel.exporter.otlp.traces.endpoint", "https://traces.example.com"),
                Map.entry("otel.exporter.otlp.metrics.endpoint", "https://metrics.example.com"),
                Map.entry("otel.exporter.otlp.logs.endpoint", "https://logs.example.com"),
                Map.entry("otel.service.name", "checkout")));

    assertProperties(
        fileContent,
        Map.ofEntries(
            Map.entry("SPLUNK_PROFILER_ENABLED", "true"),
            Map.entry("SPLUNK_PROFILER_MEMORY_ENABLED", "true"),
            Map.entry("SPLUNK_SNAPSHOT_PROFILER_ENABLED", "true"),
            Map.entry("SPLUNK_SNAPSHOT_PROFILER_SAMPLING_INTERVAL", "26ms"),
            Map.entry("SPLUNK_SNAPSHOT_SELECTION_PROBABILITY", "0.0123"),
            Map.entry("SPLUNK_PROFILER_CALL_STACK_INTERVAL", "1235ms"),
            Map.entry("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", "https://traces.example.com"),
            Map.entry("OTEL_EXPORTER_OTLP_METRICS_ENDPOINT", "https://metrics.example.com"),
            Map.entry("OTEL_EXPORTER_OTLP_LOGS_ENDPOINT", "https://logs.example.com"),
            Map.entry("OTEL_CONFIG_FILE", "null"),
            Map.entry("OTEL_EXPERIMENTAL_CONFIG_FILE", "null")));
    assertThat(fileContent.size()).isEqualTo(11);
  }

  @Test
  void buildFileContent_reportsDefaultValuesWhenNotConfigured() throws IOException {
    Properties fileContent = createFileContent(Map.ofEntries());

    assertProperties(
        fileContent,
        Map.ofEntries(
            Map.entry("SPLUNK_PROFILER_ENABLED", "false"),
            Map.entry("SPLUNK_PROFILER_MEMORY_ENABLED", "false"),
            Map.entry("SPLUNK_SNAPSHOT_PROFILER_ENABLED", "false"),
            Map.entry("SPLUNK_SNAPSHOT_PROFILER_SAMPLING_INTERVAL", "10ms"),
            Map.entry("SPLUNK_SNAPSHOT_SELECTION_PROBABILITY", "0.01"),
            Map.entry("SPLUNK_PROFILER_CALL_STACK_INTERVAL", "10000ms"),
            Map.entry("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", "http://localhost:4318/v1/traces"),
            Map.entry("OTEL_EXPORTER_OTLP_METRICS_ENDPOINT", "http://localhost:4318/v1/metrics"),
            Map.entry("OTEL_EXPORTER_OTLP_LOGS_ENDPOINT", "http://localhost:4318/v1/logs"),
            Map.entry("OTEL_CONFIG_FILE", "null"),
            Map.entry("OTEL_EXPERIMENTAL_CONFIG_FILE", "null")));
    assertThat(fileContent.size()).isEqualTo(11);
  }

  @Test
  void buildFileContent_appendsSignalPathsToBaseHttpProtobufEndpoint() throws IOException {
    Properties fileContent =
        createFileContent(
            Map.ofEntries(Map.entry("otel.exporter.otlp.endpoint", "https://collector:4318")));

    assertProperties(
        fileContent,
        Map.ofEntries(
            Map.entry("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", "https://collector:4318/v1/traces"),
            Map.entry("OTEL_EXPORTER_OTLP_METRICS_ENDPOINT", "https://collector:4318/v1/metrics"),
            Map.entry("OTEL_EXPORTER_OTLP_LOGS_ENDPOINT", "https://collector:4318/v1/logs")));
  }

  @Test
  void buildFileContent_reportsEmptySignalEndpointsWhenExportersAreNotOtlp() throws IOException {
    Properties fileContent =
        createFileContent(
            Map.ofEntries(
                Map.entry("otel.exporter.otlp.endpoint", "https://collector:4318"),
                Map.entry("otel.traces.exporter", "custom"),
                Map.entry("otel.metrics.exporter", "console"),
                Map.entry("otel.logs.exporter", "none")));

    assertProperties(
        fileContent,
        Map.ofEntries(
            Map.entry("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", ""),
            Map.entry("OTEL_EXPORTER_OTLP_METRICS_ENDPOINT", ""),
            Map.entry("OTEL_EXPORTER_OTLP_LOGS_ENDPOINT", "")));
  }

  @Test
  void buildFileContent_usesBaseGrpcEndpointForAllSignals() throws IOException {
    Properties fileContent =
        createFileContent(
            Map.ofEntries(
                Map.entry("otel.exporter.otlp.endpoint", "https://collector:4317"),
                Map.entry("otel.exporter.otlp.protocol", "grpc")));

    assertProperties(
        fileContent,
        Map.ofEntries(
            Map.entry("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", "https://collector:4317"),
            Map.entry("OTEL_EXPORTER_OTLP_METRICS_ENDPOINT", "https://collector:4317"),
            Map.entry("OTEL_EXPORTER_OTLP_LOGS_ENDPOINT", "https://collector:4317")));
  }

  @Test
  void buildFileContent_usesSignalSpecificProtocolWhenResolvingEndpoints() throws IOException {
    Properties fileContent =
        createFileContent(
            Map.ofEntries(
                Map.entry("otel.exporter.otlp.endpoint", "https://collector:4317"),
                Map.entry("otel.exporter.otlp.traces.protocol", "grpc"),
                Map.entry("otel.exporter.otlp.metrics.protocol", "grpc"),
                Map.entry("otel.exporter.otlp.logs.protocol", "grpc")));

    assertProperties(
        fileContent,
        Map.ofEntries(
            Map.entry("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", "https://collector:4317"),
            Map.entry("OTEL_EXPORTER_OTLP_METRICS_ENDPOINT", "https://collector:4317"),
            Map.entry("OTEL_EXPORTER_OTLP_LOGS_ENDPOINT", "https://collector:4317")));
  }

  private static Properties createFileContent(Map<String, String> configMap) throws IOException {
    DefaultConfigProperties config = DefaultConfigProperties.createFromMap(configMap);
    ProfilerConfiguration.SUPPLIER.configure(ProfilerEnvVarsConfigurationFactory.create(config));
    SnapshotProfilingConfiguration.SUPPLIER.configure(
        SnapshotProfilingEnvVarsConfigurationFactory.create(config));
    String fileContent =
        new EnvVarsEffectiveConfigFileFactory(config).createEffectiveConfigContent();

    Properties properties = new Properties();
    properties.load(new StringReader(fileContent));
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
