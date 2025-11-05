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
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class LogExporterBuilderTest {
  static final String DEFAULT_HTTP_LOG_ENDPOINT = "http://localhost:4318/v1/logs";
  static final String DEFAULT_GRPC_LOG_ENDPOINT = "http://localhost:4317";

  private MockedStatic<Configuration> configurationMock;

  @BeforeEach
  void setUp() {
    configurationMock = mockStatic(Configuration.class);
  }

  @AfterEach
  void tearDown() {
    configurationMock.close();
  }

  @Test
  void testBuildSimpleGrpc() {
    // given
    ConfigProperties config = mock(ConfigProperties.class);
    OtlpGrpcLogRecordExporterBuilder builder = mock(OtlpGrpcLogRecordExporterBuilder.class);
    OtlpGrpcLogRecordExporter expected = mock(OtlpGrpcLogRecordExporter.class);

    when(builder.addHeader(EXTRA_CONTENT_TYPE, STACKTRACES_HEADER_VALUE)).thenReturn(builder);
    when(builder.build()).thenReturn(expected);
    configurationMock
        .when(() -> Configuration.getIngestUrl(config))
        .thenReturn(DEFAULT_GRPC_LOG_ENDPOINT);

    // when
    LogRecordExporter exporter = LogExporterBuilder.buildGrpcExporter(config, () -> builder);

    // then
    assertThat(exporter).isSameAs(expected);
    verify(builder).setEndpoint(DEFAULT_GRPC_LOG_ENDPOINT);
  }

  @Test
  void testBuildSimpleHttp() {
    // given
    ConfigProperties config = mock(ConfigProperties.class);
    OtlpHttpLogRecordExporterBuilder builder = mock(OtlpHttpLogRecordExporterBuilder.class);
    OtlpHttpLogRecordExporter expected = mock(OtlpHttpLogRecordExporter.class);

    when(builder.addHeader(EXTRA_CONTENT_TYPE, STACKTRACES_HEADER_VALUE)).thenReturn(builder);
    when(builder.build()).thenReturn(expected);
    when(config.getString("otel.exporter.otlp.protocol", "grpc")).thenReturn("http/protobuf");
    configurationMock
        .when(() -> Configuration.getIngestUrl(config))
        .thenReturn(DEFAULT_HTTP_LOG_ENDPOINT);

    // when
    LogRecordExporter exporter = LogExporterBuilder.buildHttpExporter(config, () -> builder);

    // then
    assertThat(exporter).isSameAs(expected);
    verify(builder).setEndpoint(DEFAULT_HTTP_LOG_ENDPOINT);
  }

  @Test
  void extraOtlpHeaders() {
    // given
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getMap("otel.exporter.otlp.headers"))
        .thenReturn(Map.of("foo", "bar", "bar", "baz"));
    when(config.getMap("otel.exporter.otlp.logs.headers")).thenReturn(Map.of("log", "lady"));
    when(config.getString("otel.exporter.otlp.protocol", "grpc")).thenReturn("http/protobuf");

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
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getMap("otel.exporter.otlp.headers"))
        .thenReturn(Map.of("foo", "bar", "bar", "baz"));
    when(config.getString("otel.exporter.otlp.protocol", "grpc")).thenReturn("http/protobuf");

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
