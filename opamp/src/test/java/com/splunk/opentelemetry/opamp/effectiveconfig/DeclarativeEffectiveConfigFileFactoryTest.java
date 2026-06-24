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

import static io.opentelemetry.api.incubator.config.DeclarativeConfigProperties.empty;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getDistributionConfig;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.opamp.DeclarativeConfigurationInterceptor;
import com.splunk.opentelemetry.profiler.ProfilerConfiguration;
import com.splunk.opentelemetry.profiler.ProfilerDeclarativeConfigurationFactory;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingConfiguration;
import com.splunk.opentelemetry.profiler.snapshot.SnapshotProfilingDeclarativeConfiguration;
import com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationBuilder;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeclarativeEffectiveConfigFileFactoryTest {
  @RegisterExtension static final AutoCleanupExtension autoCleanup = AutoCleanupExtension.create();

  @Mock private ProfilerConfiguration mockProfilerConfiguration;
  @Mock private SnapshotProfilingConfiguration mockSnapshotConfiguration;

  @AfterEach
  void afterEach() {
    DeclarativeConfigurationInterceptor.reset();
    ProfilerConfiguration.SUPPLIER.reset();
    SnapshotProfilingDeclarativeConfiguration.SUPPLIER.reset();
  }

  @Test
  void buildFileContent_handleRealConfigPaths(@TempDir Path tempDir) throws Exception {
    // given
    Path configFile = tempDir.resolve("declarative-config.yaml");
    Files.writeString(configFile, "file_format: 1.0", UTF_8);

    ProcessBuilder processBuilder =
        new ProcessBuilder(
                System.getProperty("java.home") + "/bin/java",
                "-cp",
                System.getProperty("java.class.path"),
                "-Dotel.config.file=" + configFile,
                "-Dotel.experimental.config.file=test.yaml",
                FactoryRunner.class.getName())
            .redirectErrorStream(true);

    // when
    Process process = processBuilder.start();
    boolean processExecutionStatus = process.waitFor(10, TimeUnit.SECONDS);

    // then
    assertThat(processExecutionStatus).isTrue();
    String fileContent = new String(process.getInputStream().readAllBytes(), UTF_8);
    assertThat(process.exitValue()).describedAs(fileContent).isZero();

    assertThat(fileContent)
        .matches(
            """
            otel_config_file: .*declarative-config\\.yaml
            otel_experimental_config_file: test\\.yaml
            """);
  }

  @Test
  void handlesDistributionConfig(@TempDir Path tempDir) throws Exception {
    // given
    String configurationYaml =
        """
        file_format: 1.0
        distribution:
          splunk:
            profiling:
              always_on:
                cpu_profiler:
                  sampling_interval: 1410
                memory_profiler:
              callgraphs:
                sampling_interval: 10
        """;

    // when
    DeclarativeConfigTestUtil.createAutoConfiguredSdk(configurationYaml, tempDir, autoCleanup);
    String effectiveConfigYaml =
        new DeclarativeEffectiveConfigFileFactory().createEffectiveConfigContent();

    // then
    assertThat(effectiveConfigYaml)
        .isEqualTo(
            """
            otel_config_file: null
            otel_experimental_config_file: null
            distribution:
              splunk:
                profiling:
                  always_on:
                    cpu_profiler:
                      sampling_interval: 1410
                    memory_profiler:
                  callgraphs:
                    sampling_interval: 10
            """);
  }

  @Test
  void handlesBlankConfig() throws Exception {
    OpenTelemetryConfigurationModel model = parseModel("file_format: 1.0");

    String yaml =
        new DeclarativeEffectiveConfigFileFactory()
            .processModel(model, mockProfilerConfiguration, mockSnapshotConfiguration);

    assertThat(yaml)
        .isEqualTo(
            """
            otel_config_file: null
            otel_experimental_config_file: null
            """);
  }

  @Test
  void getFileName_usesDeclarativeConfigFileName() {
    String previousConfigFile = System.getProperty("otel.config.file");
    try {
      System.setProperty("otel.config.file", "/opt/splunk/declarative-config.yaml");

      String fileName = new DeclarativeEffectiveConfigFileFactory().getFileName();

      assertThat(fileName).isEqualTo("/opt/splunk/declarative-config.yaml");
    } finally {
      if (previousConfigFile == null) {
        System.clearProperty("otel.config.file");
      } else {
        System.setProperty("otel.config.file", previousConfigFile);
      }
    }
  }

  @Test
  void usesDefaultHttpEndpointsWhenEndpointsAreOmitted() throws Exception {
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

    String yaml =
        new DeclarativeEffectiveConfigFileFactory()
            .processModel(model, mockProfilerConfiguration, mockSnapshotConfiguration);

    assertThat(yaml)
        .isEqualTo(
            """
            otel_config_file: null
            otel_experimental_config_file: null
            tracer_provider:
              processors:
                - batch:
                    exporter:
                      otlp_http:
                        endpoint: http://localhost:4318/v1/traces
            meter_provider:
              readers:
                - periodic:
                    exporter:
                      otlp_http:
                        endpoint: http://localhost:4318/v1/metrics
            logger_provider:
              processors:
                - simple:
                    exporter:
                      otlp_http:
                        endpoint: http://localhost:4318/v1/logs
            """);
  }

  @Test
  void usesDefaultGrpcEndpointsWhenEndpointsAreOmitted() throws Exception {
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

    String yaml =
        new DeclarativeEffectiveConfigFileFactory()
            .processModel(model, mockProfilerConfiguration, mockSnapshotConfiguration);

    assertThat(yaml)
        .isEqualTo(
            """
            otel_config_file: null
            otel_experimental_config_file: null
            tracer_provider:
              processors:
                - batch:
                    exporter:
                      otlp_grpc:
                        endpoint: http://localhost:4317
            meter_provider:
              readers:
                - periodic:
                    exporter:
                      otlp_grpc:
                        endpoint: http://localhost:4317
            logger_provider:
              processors:
                - simple:
                    exporter:
                      otlp_grpc:
                        endpoint: http://localhost:4317
            """);
  }

  @Test
  void supportsMultipleEndpointsDefined() throws Exception {
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
                      console:
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
                - simple:
                    exporter:
                      console:
            """);

    String yaml =
        new DeclarativeEffectiveConfigFileFactory()
            .processModel(model, mockProfilerConfiguration, mockSnapshotConfiguration);

    assertThat(yaml)
        .isEqualTo(
            """
            otel_config_file: null
            otel_experimental_config_file: null
            tracer_provider:
              processors:
                - batch:
                    exporter:
                      otlp_http:
                        endpoint: https://traces.example.com
                - simple:
                    exporter:
                      otlp_grpc:
                        endpoint: http://localhost:4317
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
  }

  @Test
  void supportsAlwaysOnProfiler() throws Exception {
    OpenTelemetryConfigurationModel model = parseModel("file_format: 1.0");
    when(mockProfilerConfiguration.isEnabled()).thenReturn(true);
    when(mockProfilerConfiguration.getCallStackInterval()).thenReturn(Duration.ofMillis(1410));
    when(mockProfilerConfiguration.getMemoryEnabled()).thenReturn(true);
    when(mockSnapshotConfiguration.isEnabled()).thenReturn(true);
    when(mockSnapshotConfiguration.getSamplingInterval()).thenReturn(Duration.ofMillis(10));

    String yaml =
        new DeclarativeEffectiveConfigFileFactory()
            .processModel(model, mockProfilerConfiguration, mockSnapshotConfiguration);

    assertThat(yaml)
        .isEqualTo(
            """
            otel_config_file: null
            otel_experimental_config_file: null
            distribution:
              splunk:
                profiling:
                  always_on:
                    cpu_profiler:
                      sampling_interval: 1410
                    memory_profiler:
                  callgraphs:
                    sampling_interval: 10
            """);
  }

  private static class FactoryRunner {
    public static void main(String[] args) throws Exception {
      Path configFile = Path.of(System.getProperty("otel.config.file"));
      try (InputStream inputStream = Files.newInputStream(configFile)) {
        OpenTelemetryConfigurationModel model = DeclarativeConfiguration.parse(inputStream);

        DeclarativeConfigProperties profilingConfig =
            getDistributionConfig(model).getStructured("profiling", empty());
        ProfilerConfiguration.SUPPLIER.configure(
            ProfilerDeclarativeConfigurationFactory.create(profilingConfig));
        SnapshotProfilingDeclarativeConfiguration.SUPPLIER.configure(
            new SnapshotProfilingDeclarativeConfiguration(profilingConfig));

        DeclarativeConfigurationBuilder builder = new DeclarativeConfigurationBuilder();
        new DeclarativeConfigurationInterceptor().customize(builder);
        builder.customizeModel(model);
      }

      System.out.print(new DeclarativeEffectiveConfigFileFactory().createEffectiveConfigContent());
    }
  }

  private static OpenTelemetryConfigurationModel parseModel(String yaml) throws Exception {
    return DeclarativeConfiguration.parse(new ByteArrayInputStream(yaml.getBytes(UTF_8)));
  }
}
