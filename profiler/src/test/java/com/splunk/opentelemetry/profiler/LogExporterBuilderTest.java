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

import static com.splunk.opentelemetry.profiler.LogExporterBuilder.EXTRA_CONTENT_TYPE;
import static com.splunk.opentelemetry.profiler.LogExporterBuilder.STACKTRACES_HEADER_VALUE;
import static com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil.getProfilingConfig;
import static io.opentelemetry.api.incubator.config.DeclarativeConfigProperties.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.YamlDeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.GrpcSender;
import io.opentelemetry.sdk.common.export.GrpcSenderProvider;
import io.opentelemetry.sdk.common.export.HttpSender;
import io.opentelemetry.sdk.common.export.HttpSenderProvider;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LogExporterBuilderTest {

  @Nested
  class EnvVarBasedConfig {
    static final String DEFAULT_HTTP_LOG_ENDPOINT = "http://localhost:4318/v1/logs";
    static final String DEFAULT_GRPC_LOG_ENDPOINT = "http://localhost:4317";

    @Test
    void testBuildSimpleGrpc() {
      // given
      ProfilerConfiguration config = mock(ProfilerConfiguration.class);
      OtlpGrpcLogRecordExporterBuilder builder = mock(OtlpGrpcLogRecordExporterBuilder.class);
      OtlpGrpcLogRecordExporter expected = mock(OtlpGrpcLogRecordExporter.class);

      when(builder.addHeader(EXTRA_CONTENT_TYPE, STACKTRACES_HEADER_VALUE)).thenReturn(builder);
      when(builder.build()).thenReturn(expected);
      when(config.getIngestUrl()).thenReturn(DEFAULT_GRPC_LOG_ENDPOINT);

      // when
      LogRecordExporter exporter = LogExporterBuilder.buildGrpcExporter(config, () -> builder);

      // then
      assertThat(exporter).isSameAs(expected);
      verify(builder).setEndpoint(DEFAULT_GRPC_LOG_ENDPOINT);
    }

    @Test
    void testBuildSimpleHttp() {
      // given
      ProfilerConfiguration config = mock(ProfilerConfiguration.class);
      ConfigProperties configProperties = mock(ConfigProperties.class);
      OtlpHttpLogRecordExporterBuilder builder = mock(OtlpHttpLogRecordExporterBuilder.class);
      OtlpHttpLogRecordExporter expected = mock(OtlpHttpLogRecordExporter.class);

      when(builder.addHeader(EXTRA_CONTENT_TYPE, STACKTRACES_HEADER_VALUE)).thenReturn(builder);
      when(builder.build()).thenReturn(expected);
      when(config.getConfigProperties()).thenReturn(configProperties);
      when(configProperties.getString(eq("otel.exporter.otlp.protocol"), anyString()))
          .thenReturn("http/protobuf");
      when(config.getIngestUrl()).thenReturn(DEFAULT_HTTP_LOG_ENDPOINT);

      // when
      LogRecordExporter exporter = LogExporterBuilder.buildHttpExporter(config, () -> builder);

      // then
      assertThat(exporter).isSameAs(expected);
      verify(builder).setEndpoint(DEFAULT_HTTP_LOG_ENDPOINT);
    }

    @Test
    void extraOtlpHeaders() {
      // given
      ProfilerConfiguration config = mock(ProfilerConfiguration.class);
      ConfigProperties configProperties = mock(ConfigProperties.class);
      when(config.getConfigProperties()).thenReturn(configProperties);
      when(config.getOtlpProtocol()).thenReturn("http/protobuf");
      when(config.getIngestUrl()).thenReturn(DEFAULT_HTTP_LOG_ENDPOINT);
      when(configProperties.getMap("otel.exporter.otlp.headers"))
          .thenReturn(Map.of("foo", "bar", "bar", "baz"));
      when(configProperties.getMap("otel.exporter.otlp.logs.headers"))
          .thenReturn(Map.of("log", "lady"));
      when(configProperties.getString("otel.exporter.otlp.protocol", "grpc"))
          .thenReturn("http/protobuf");

      OtlpHttpLogRecordExporterBuilder builder = mock(OtlpHttpLogRecordExporterBuilder.class);
      OtlpHttpLogRecordExporter expected = mock(OtlpHttpLogRecordExporter.class);

      when(builder.addHeader(anyString(), anyString())).thenReturn(builder);
      when(builder.build()).thenReturn(expected);

      // when
      LogRecordExporter exporter = LogExporterBuilder.buildHttpExporter(config, () -> builder);

      // then
      assertThat(exporter).isSameAs(expected);
      verify(builder).addHeader(EXTRA_CONTENT_TYPE, STACKTRACES_HEADER_VALUE);
      verify(builder).addHeader("log", "lady");
      verify(builder, never()).addHeader("foo", "bar");
      verify(builder, never()).addHeader("bar", "baz");
    }

    @Test
    void extraOtlpLogSpecificHeaders() {
      // given
      ProfilerConfiguration config = mock(ProfilerConfiguration.class);
      ConfigProperties configProperties = mock(ConfigProperties.class);
      when(config.getConfigProperties()).thenReturn(configProperties);
      when(config.getOtlpProtocol()).thenReturn("http/protobuf");
      when(config.getIngestUrl()).thenReturn(DEFAULT_HTTP_LOG_ENDPOINT);
      when(configProperties.getMap("otel.exporter.otlp.headers"))
          .thenReturn(Map.of("foo", "bar", "bar", "baz"));
      when(configProperties.getString("otel.exporter.otlp.protocol", "grpc"))
          .thenReturn("http/protobuf");

      OtlpHttpLogRecordExporterBuilder builder = mock(OtlpHttpLogRecordExporterBuilder.class);
      OtlpHttpLogRecordExporter expected = mock(OtlpHttpLogRecordExporter.class);

      when(builder.addHeader(anyString(), anyString())).thenReturn(builder);
      when(builder.build()).thenReturn(expected);

      // when
      LogRecordExporter exporter = LogExporterBuilder.buildHttpExporter(config, () -> builder);

      // then
      assertThat(exporter).isSameAs(expected);
      verify(builder).addHeader(EXTRA_CONTENT_TYPE, STACKTRACES_HEADER_VALUE);
    }
  }

  @Nested
  class DeclarativeConfig {

    @ParameterizedTest
    @ValueSource(strings = {"otlp_log_http", "otlp_log_grpc", "default"})
    void shouldAddProfilingHeaderAutomatically(String exporterType) {
      // given
      CapturingComponentLoader componentLoader = new CapturingComponentLoader();
      Map<String, Object> properties = new HashMap<>();
      if (!"default".equals(exporterType)) {
        properties.put(exporterType, null);
      }
      DeclarativeConfigProperties exporterConfig =
          YamlDeclarativeConfigProperties.create(properties, componentLoader);

      // when
      try (LogRecordExporter exporter = LogExporterBuilder.fromDeclarativeConfig(exporterConfig)) {
        // then
        assertThat(exporter).isNotNull();
        assertThat(componentLoader.headers)
            .containsEntry(EXTRA_CONTENT_TYPE, List.of(STACKTRACES_HEADER_VALUE));
        assertThat(componentLoader.endpoint)
            .isEqualTo(
                URI.create(
                    "otlp_log_grpc".equals(exporterType)
                        ? "http://localhost:4317"
                        : "http://localhost:4318/v1/logs"));
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {"otlp_log_http", "otlp_log_grpc"})
    void shouldPreserveConfiguredSettingsAndSetProfilingHeader(String exporterType) {
      // given
      CapturingComponentLoader componentLoader = new CapturingComponentLoader();
      Map<String, Object> exporterSettings =
          Map.of(
              "endpoint", "http://localhost:1234",
              "timeout", 1234,
              "compression", "gzip",
              "headers_list", "X-From-List=one",
              "headers",
                  List.of(
                      Map.of("name", "X-Custom-Header", "value", "two"),
                      Map.of("name", EXTRA_CONTENT_TYPE, "value", "custom")));
      DeclarativeConfigProperties exporterConfig =
          YamlDeclarativeConfigProperties.create(
              Map.of(exporterType, exporterSettings), componentLoader);

      // when
      try (LogRecordExporter exporter = LogExporterBuilder.fromDeclarativeConfig(exporterConfig)) {
        // then
        assertThat(exporter).isNotNull();
        assertThat(componentLoader.headers)
            .containsEntry(EXTRA_CONTENT_TYPE, List.of(STACKTRACES_HEADER_VALUE))
            .containsEntry("X-From-List", List.of("one"))
            .containsEntry("X-Custom-Header", List.of("two"));
        assertThat(componentLoader.endpoint).isEqualTo(URI.create("http://localhost:1234"));
        assertThat(componentLoader.timeout).isEqualTo(Duration.ofMillis(1234));
        assertThat(componentLoader.compression).isEqualTo("gzip");
      }
    }

    @Test
    void shouldCreateHttpExporter() {
      // given
      OpenTelemetryConfigurationModel model =
          DeclarativeConfigTestUtil.parse(
              """
            file_format: "1.1"
            distribution:
              splunk:
                profiling:
                  exporter:
                    otlp_log_http:
                      endpoint: "http://acme.com"
          """);

      DeclarativeConfigProperties exporterConfig = getExporterConfig(model);

      // when
      LogRecordExporter exporter = LogExporterBuilder.fromDeclarativeConfig(exporterConfig);

      // then
      assertThat(exporter).isNotNull();
    }

    @Test
    void shouldCreateGrpcExporter() {
      // given
      OpenTelemetryConfigurationModel model =
          DeclarativeConfigTestUtil.parse(
              """
                file_format: "1.1"
                distribution:
                  splunk:
                    profiling:
                      exporter:
                        otlp_log_grpc:
                          endpoint: "http://acme.com"
              """);

      DeclarativeConfigProperties exporterConfig = getExporterConfig(model);

      // when
      LogRecordExporter exporter = LogExporterBuilder.fromDeclarativeConfig(exporterConfig);

      // then
      assertThat(exporter).isNotNull();
    }

    @Test
    void shouldCreateHttpExporter_defaultEndpoint() {
      // given
      OpenTelemetryConfigurationModel model =
          DeclarativeConfigTestUtil.parse(
              """
            file_format: "1.1"
            distribution:
              splunk:
                profiling:
                  exporter:
                    otlp_log_http:
          """);

      DeclarativeConfigProperties exporterConfig = getExporterConfig(model);

      // when
      LogRecordExporter exporter = LogExporterBuilder.fromDeclarativeConfig(exporterConfig);

      // then
      assertThat(exporter).isNotNull();
    }

    @Test
    void shouldCreateGrpcExporter_defaultEndpoint() {
      // given
      OpenTelemetryConfigurationModel model =
          DeclarativeConfigTestUtil.parse(
              """
                file_format: "1.1"
                distribution:
                  splunk:
                    profiling:
                      exporter:
                        otlp_log_grpc:
              """);

      DeclarativeConfigProperties exporterConfig = getExporterConfig(model);

      // when
      LogRecordExporter exporter = LogExporterBuilder.fromDeclarativeConfig(exporterConfig);

      // then
      assertThat(exporter).isNotNull();
    }

    @Test
    void shouldCreateHttpExporterWithEmptyNodeUsingParentComponentLoader() {
      // given
      RecordingComponentLoader componentLoader = new RecordingComponentLoader();
      DeclarativeConfigProperties exporterConfig =
          exporterConfigWithEmptyNode("otlp_log_http", componentLoader);

      // when
      LogRecordExporter exporter = LogExporterBuilder.fromDeclarativeConfig(exporterConfig);

      // then
      assertThat(exporter).isNotNull();
      assertThat(componentLoader.loaded(HttpSenderProvider.class)).isTrue();
    }

    @Test
    void shouldCreateGrpcExporterWithEmptyNodeUsingParentComponentLoader() {
      // given
      RecordingComponentLoader componentLoader = new RecordingComponentLoader();
      DeclarativeConfigProperties exporterConfig =
          exporterConfigWithEmptyNode("otlp_log_grpc", componentLoader);

      // when
      LogRecordExporter exporter = LogExporterBuilder.fromDeclarativeConfig(exporterConfig);

      // then
      assertThat(exporter).isNotNull();
      assertThat(componentLoader.loaded(GrpcSenderProvider.class)).isTrue();
    }

    @Test
    void shouldThrowExceptionForInvalidProtocol() {
      // given
      OpenTelemetryConfigurationModel model =
          DeclarativeConfigTestUtil.parse(
              """
                file_format: "1.1"
                distribution:
                  splunk:
                    profiling:
                      exporter:
                        unsupported:
              """);

      DeclarativeConfigProperties exporterConfig = getExporterConfig(model);

      // when, then
      assertThatThrownBy(() -> LogExporterBuilder.fromDeclarativeConfig(exporterConfig))
          .isInstanceOf(ConfigurationException.class);
    }

    private static DeclarativeConfigProperties getExporterConfig(
        OpenTelemetryConfigurationModel model) {
      return getProfilingConfig(model).getStructured("exporter", empty());
    }

    private static DeclarativeConfigProperties exporterConfigWithEmptyNode(
        String exporterName, ComponentLoader componentLoader) {
      Map<String, Object> properties = new HashMap<>();
      properties.put(exporterName, null);
      return YamlDeclarativeConfigProperties.create(properties, componentLoader);
    }

    private static class CapturingComponentLoader implements ComponentLoader {
      private final ComponentLoader delegate =
          ComponentLoader.forClassLoader(LogExporterBuilderTest.class.getClassLoader());
      private Map<String, List<String>> headers;
      private URI endpoint;
      private Duration timeout;
      private String compression;

      @Override
      public <T> Iterable<T> load(Class<T> spiClass) {
        if (spiClass == HttpSenderProvider.class) {
          HttpSenderProvider provider =
              config -> {
                headers = config.getHeadersSupplier().get();
                endpoint = config.getEndpoint();
                timeout = config.getTimeout();
                compression =
                    config.getCompressor() == null ? null : config.getCompressor().getEncoding();
                HttpSender sender = mock(HttpSender.class);
                when(sender.shutdown()).thenReturn(CompletableResultCode.ofSuccess());
                return sender;
              };
          return List.of(spiClass.cast(provider));
        }
        if (spiClass == GrpcSenderProvider.class) {
          GrpcSenderProvider provider =
              config -> {
                headers = config.getHeadersSupplier().get();
                endpoint = config.getEndpoint();
                timeout = config.getTimeout();
                compression =
                    config.getCompressor() == null ? null : config.getCompressor().getEncoding();
                GrpcSender sender = mock(GrpcSender.class);
                when(sender.shutdown()).thenReturn(CompletableResultCode.ofSuccess());
                return sender;
              };
          return List.of(spiClass.cast(provider));
        }
        return delegate.load(spiClass);
      }
    }

    private static class RecordingComponentLoader implements ComponentLoader {
      private final ComponentLoader delegate =
          ComponentLoader.forClassLoader(LogExporterBuilderTest.class.getClassLoader());
      private final List<Class<?>> loadedSpiClasses = new ArrayList<>();

      @Override
      public <T> Iterable<T> load(Class<T> spiClass) {
        loadedSpiClasses.add(spiClass);
        return delegate.load(spiClass);
      }

      boolean loaded(Class<?> spiClass) {
        return loadedSpiClasses.contains(spiClass);
      }
    }
  }
}
