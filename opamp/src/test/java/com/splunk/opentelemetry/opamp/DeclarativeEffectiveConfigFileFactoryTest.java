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

import static io.opentelemetry.api.incubator.config.DeclarativeConfigProperties.empty;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getDistributionConfig;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.splunk.opentelemetry.profiler.ProfilerDeclarativeConfiguration;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingDeclarativeConfiguration;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationBuilder;
import io.opentelemetry.sdk.declarativeconfig.internal.model.OpenTelemetryConfigurationModel;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeclarativeEffectiveConfigFileFactoryTest {

  @AfterEach
  void afterEach() {
    DeclarativeConfigurationInterceptor.reset();
    ProfilerDeclarativeConfiguration.SUPPLIER.reset();
    SnapshotProfilingDeclarativeConfiguration.SUPPLIER.reset();
  }

  @Test
  void buildFileContent_reportsSignalEndpoints(@TempDir Path tempDir) throws Exception {
    // given
    Path configFile = tempDir.resolve("declarative-config.yaml");
    Files.writeString(
        configFile,
        """
            file_format: 1.0
            tracer_provider:
              processors:
                - batch:
                    exporter:
                      otlp_http:
                        endpoint: https://traces.example.com
            meter_provider:
              readers:
                - periodic:
                    exporter:
                      otlp_grpc:
                        endpoint: https://metrics.example.com
            logger_provider:
              processors:
                - simple:
                    exporter:
                      otlp_http:
                        endpoint: https://logs.example.com
            distribution:
              splunk:
                profiling:
                  always_on:
                    cpu_profiler:
                      sampling_interval: 1410
                    memory_profiler:
                  callgraphs:
                    sampling_interval: 10
            """,
        UTF_8);

    ProcessBuilder processBuilder =
        new ProcessBuilder(
                System.getProperty("java.home") + "/bin/java",
                "-cp",
                System.getProperty("java.class.path"),
                "-Dotel.config.file=" + configFile,
                FactoryRunner.class.getName())
            .redirectErrorStream(true);

    // when
    Process process = processBuilder.start();
    boolean processExecutionStatus = process.waitFor(10, TimeUnit.SECONDS);

    // then
    assertThat(processExecutionStatus).isTrue();
    String fileContent = new String(process.getInputStream().readAllBytes(), UTF_8);
    assertThat(process.exitValue()).describedAs(fileContent).isZero();

    Properties properties = loadProperties(fileContent);
    assertProperties(
        properties,
        Map.of(
            "OTEL_EXPORTER_OTLP_TRACES_ENDPOINTS", "\"https://traces.example.com\"",
            "OTEL_EXPORTER_OTLP_METRICS_ENDPOINTS", "\"https://metrics.example.com\"",
            "OTEL_EXPORTER_OTLP_LOGS_ENDPOINTS", "\"https://logs.example.com\""));
  }

  @Test
  void addOtelVars_reportsMissingEndpointAsEmptyStrings() throws Exception {
    OpenTelemetryConfigurationModel model = parseModel("file_format: 1.0");

    EffectiveConfigBuilder builder = new EffectiveConfigBuilder();
    new DeclarativeEffectiveConfigFileFactory().addOtelVars(builder, model);

    Properties properties = loadProperties(builder.build());
    assertProperties(
        properties,
        Map.of(
            "OTEL_EXPORTER_OTLP_TRACES_ENDPOINTS", "",
            "OTEL_EXPORTER_OTLP_METRICS_ENDPOINTS", "",
            "OTEL_EXPORTER_OTLP_LOGS_ENDPOINTS", ""));
  }

  @Test
  void addOtelVars_usesDefaultHttpEndpointsWhenEndpointsAreOmitted() throws Exception {
    OpenTelemetryConfigurationModel model =
        parseModel(
            """
            file_format: 1.0
            tracer_provider:
              processors:
                - batch:
                    exporter:
                      otlp_http:
            meter_provider:
              readers:
                - periodic:
                    exporter:
                      otlp_http:
            logger_provider:
              processors:
                - simple:
                    exporter:
                      otlp_http:
            """);

    EffectiveConfigBuilder builder = new EffectiveConfigBuilder();
    new DeclarativeEffectiveConfigFileFactory().addOtelVars(builder, model);

    Properties properties = loadProperties(builder.build());
    assertProperties(
        properties,
        Map.of(
            "OTEL_EXPORTER_OTLP_TRACES_ENDPOINTS", "\"http://localhost:4318/v1/traces\"",
            "OTEL_EXPORTER_OTLP_METRICS_ENDPOINTS", "\"http://localhost:4318/v1/metrics\"",
            "OTEL_EXPORTER_OTLP_LOGS_ENDPOINTS", "\"http://localhost:4318/v1/logs\""));
  }

  @Test
  void addOtelVars_usesDefaultGrpcEndpointsWhenEndpointsAreOmitted() throws Exception {
    OpenTelemetryConfigurationModel model =
        parseModel(
            """
            file_format: 1.0
            tracer_provider:
              processors:
                - batch:
                    exporter:
                      otlp_grpc:
            meter_provider:
              readers:
                - periodic:
                    exporter:
                      otlp_grpc:
            logger_provider:
              processors:
                - simple:
                    exporter:
                      otlp_grpc:
            """);

    EffectiveConfigBuilder builder = new EffectiveConfigBuilder();
    new DeclarativeEffectiveConfigFileFactory().addOtelVars(builder, model);

    Properties properties = loadProperties(builder.build());
    assertProperties(
        properties,
        Map.of(
            "OTEL_EXPORTER_OTLP_TRACES_ENDPOINTS", "\"http://localhost:4317\"",
            "OTEL_EXPORTER_OTLP_METRICS_ENDPOINTS", "\"http://localhost:4317\"",
            "OTEL_EXPORTER_OTLP_LOGS_ENDPOINTS", "\"http://localhost:4317\""));
  }

  @Test
  void addOtelVars_whenMultipleEndpointsDefined() throws Exception {
    OpenTelemetryConfigurationModel model =
        parseModel(
            """
            file_format: 1.0
            tracer_provider:
              processors:
                - batch:
                    exporter:
                      otlp_http:
                        endpoint: https://traces.example.com
                - simple:
                    exporter:
                      otlp_grpc:
            meter_provider:
              readers:
                - periodic:
                    exporter:
                      otlp_grpc:
                        endpoint: https://metrics.example.com
                - periodic:
                    exporter:
                      otlp_http:
                        endpoint: https://acme.com/
            logger_provider:
              processors:
                - simple:
                    exporter:
                      otlp_http:
                        endpoint: https://logs.example.com
                - batch:
                    exporter:
                      otlp_grpc:
                        endpoint: https://acme.com
            """);

    EffectiveConfigBuilder builder = new EffectiveConfigBuilder();
    new DeclarativeEffectiveConfigFileFactory().addOtelVars(builder, model);

    Properties properties = loadProperties(builder.build());
    assertProperties(
        properties,
        Map.of(
            "OTEL_EXPORTER_OTLP_TRACES_ENDPOINTS",
                "\"https://traces.example.com\", \"http://localhost:4317\"",
            "OTEL_EXPORTER_OTLP_METRICS_ENDPOINTS",
                "\"https://metrics.example.com\", \"https://acme.com/\"",
            "OTEL_EXPORTER_OTLP_LOGS_ENDPOINTS",
                "\"https://logs.example.com\", \"https://acme.com\""));
  }

  private static class FactoryRunner {
    public static void main(String[] args) throws Exception {
      Path configFile = Path.of(System.getProperty("otel.config.file"));
      try (InputStream inputStream = Files.newInputStream(configFile)) {
        OpenTelemetryConfigurationModel model = DeclarativeConfiguration.parse(inputStream);

        DeclarativeConfigProperties profilingConfig =
            getDistributionConfig(model).getStructured("profiling", empty());
        ProfilerDeclarativeConfiguration.SUPPLIER.configure(
            new ProfilerDeclarativeConfiguration(profilingConfig));
        SnapshotProfilingDeclarativeConfiguration.SUPPLIER.configure(
            new SnapshotProfilingDeclarativeConfiguration(profilingConfig));

        DeclarativeConfigurationBuilder builder = new DeclarativeConfigurationBuilder();
        new DeclarativeConfigurationInterceptor().customize(builder);
        builder.customizeModel(model);
      }

      System.out.print(new DeclarativeEffectiveConfigFileFactory().buildFileContent());
    }
  }

  private static OpenTelemetryConfigurationModel parseModel(String yaml) throws Exception {
    return DeclarativeConfiguration.parse(new ByteArrayInputStream(yaml.getBytes(UTF_8)));
  }

  private static Properties loadProperties(String content) throws Exception {
    Properties properties = new Properties();
    properties.load(new StringReader(content));
    return properties;
  }

  private static void assertProperties(Properties fileContent, Map<String, String> expectedValues) {
    expectedValues.forEach(
        (propertyName, expectedValue) ->
            assertThat(fileContent.getProperty(propertyName)).isNotNull().isEqualTo(expectedValue));
  }
}
