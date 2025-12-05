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
import static io.opentelemetry.api.incubator.config.DeclarativeConfigProperties.empty;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.extension.incubator.fileconfig.YamlDeclarativeConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LogExporterBuilderTest {

  @Nested
  class EnvVarBasedConfig {
    static final String DEFAULT_HTTP_LOG_ENDPOINT = "http://localhost:4318/v1/logs";
    static final String DEFAULT_GRPC_LOG_ENDPOINT = "http://localhost:4317";

    @Test
    void testBuildSimpleGrpc() {
      // given
      ProfilerEnvVarsConfiguration config = mock(ProfilerEnvVarsConfiguration.class);
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
      ProfilerEnvVarsConfiguration config = mock(ProfilerEnvVarsConfiguration.class);
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
      ProfilerEnvVarsConfiguration config = mock(ProfilerEnvVarsConfiguration.class);
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
      ProfilerEnvVarsConfiguration config = mock(ProfilerEnvVarsConfiguration.class);
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

    @Test
    void shouldCreateHttpExporter() {
      // given
      OpenTelemetryConfigurationModel model =
          DeclarativeConfigTestUtil.parse(
          """
            file_format: "1.0-rc.2"
            instrumentation/development:
              java:
                distribution:
                  splunk:
                    profiling:
                      exporter:
                        otlp_http:
                          endpoint: "http://acme.com"
          """
          );

      DeclarativeConfigProperties exporterConfig = getExporterConfig(model);

      // when
      LogRecordExporter exporter = LogExporterBuilder.fromConfig(exporterConfig);

      // then
      assertThat(exporter).isNotNull();
    }

    @Test
    void shouldCreateGrpcExporter() {
      // given
      OpenTelemetryConfigurationModel model =
          DeclarativeConfigTestUtil.parse(
              """
                file_format: "1.0-rc.2"
                instrumentation/development:
                  java:
                    distribution:
                      splunk:
                        profiling:
                          exporter:
                            otlp_grpc:
                              endpoint: "http://acme.com"
              """
          );

      DeclarativeConfigProperties exporterConfig = getExporterConfig(model);

      // when
      LogRecordExporter exporter = LogExporterBuilder.fromConfig(exporterConfig);

      // then
      assertThat(exporter).isNotNull();
    }

    @Test
    void shouldThrowExceptionForInvalidProtocol() {
      // given
      OpenTelemetryConfigurationModel model =
          DeclarativeConfigTestUtil.parse(
              """
                file_format: "1.0-rc.2"
                instrumentation/development:
                  java:
                    distribution:
                      splunk:
                        profiling:
                          exporter:
                            unsupported:
              """
          );

      DeclarativeConfigProperties exporterConfig = getExporterConfig(model);

      // when, then
      assertThatThrownBy(() -> LogExporterBuilder.fromConfig(exporterConfig)).isInstanceOf(ConfigurationException.class);
    }

    private static DeclarativeConfigProperties getExporterConfig(OpenTelemetryConfigurationModel model) {
      Map<String, Object> properties = model.getInstrumentationDevelopment().getJava()
          .getAdditionalProperties();
      ComponentLoader componentLoader = ComponentLoader.forClassLoader(DeclarativeConfigProperties.class.getClassLoader());
      DeclarativeConfigProperties declarativeConfigProperties = YamlDeclarativeConfigProperties.create(properties, componentLoader);
      return declarativeConfigProperties
          .getStructured("distribution", empty())
          .getStructured("splunk", empty())
          .getStructured("profiling", empty())
          .getStructured("exporter", empty());
    }
  }
}
