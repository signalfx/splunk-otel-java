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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProfilerEnvVarsConfigurationTest {
  private static final ComponentLoader COMPONENT_LOADER =
      ComponentLoader.forClassLoader(ProfilerEnvVarsConfigurationTest.class.getClassLoader());

  String logsEndpoint = "http://logs.example.com";
  String otelEndpoint = "http://otel.example.com";
  String defaultLogsEndpoint = "http://localhost:4318/v1/logs";

  @Test
  void getIngestUrl_endpointDefined() {
    // given
    ConfigProperties config = mock(ConfigProperties.class);
    ProfilerEnvVarsConfiguration profilerConfiguration = new ProfilerEnvVarsConfiguration(config);
    when(config.getString(ProfilerEnvVarsConfiguration.CONFIG_KEY_INGEST_URL))
        .thenReturn(logsEndpoint);

    // when
    String result = profilerConfiguration.getIngestUrl();

    // then
    assertThat(result).isEqualTo(logsEndpoint);
  }

  @Test
  void getIngestUrl_endpointNotDefined_usedOtelGrpc() {
    // given
    ConfigProperties config = mock(ConfigProperties.class);
    ProfilerEnvVarsConfiguration profilerConfiguration = new ProfilerEnvVarsConfiguration(config);
    when(config.getString(ProfilerEnvVarsConfiguration.CONFIG_KEY_INGEST_URL)).thenReturn(null);
    when(config.getString(eq(ProfilerEnvVarsConfiguration.CONFIG_KEY_OTEL_OTLP_URL), anyString()))
        .thenReturn(otelEndpoint);
    when(config.getString(
            eq(ProfilerEnvVarsConfiguration.CONFIG_KEY_PROFILER_OTLP_PROTOCOL), any()))
        .thenReturn("grpc");

    // when
    String result = profilerConfiguration.getIngestUrl();

    // then
    assertThat(result).isEqualTo(otelEndpoint);
  }

  @Test
  void getIngestUrl_endpointNotDefined_usedOtelHttpProtobuf() {
    // given
    ConfigProperties config = mock(ConfigProperties.class);
    ProfilerEnvVarsConfiguration profilerConfiguration = new ProfilerEnvVarsConfiguration(config);
    when(config.getString(ProfilerEnvVarsConfiguration.CONFIG_KEY_INGEST_URL)).thenReturn(null);
    when(config.getString(eq(ProfilerEnvVarsConfiguration.CONFIG_KEY_OTEL_OTLP_URL), anyString()))
        .thenReturn(otelEndpoint);
    when(config.getString(
            eq(ProfilerEnvVarsConfiguration.CONFIG_KEY_PROFILER_OTLP_PROTOCOL), any()))
        .thenReturn("http/protobuf");

    // when
    String result = profilerConfiguration.getIngestUrl();

    // then
    assertThat(result).isEqualTo(otelEndpoint + "/v1/logs");
  }

  @Test
  void getIngestUrl_endpointNotDefined_usedOtelHttpProtobufWithPath() {
    // given
    String endpoint = otelEndpoint + "/v1/logs";

    ConfigProperties config = mock(ConfigProperties.class);
    ProfilerEnvVarsConfiguration profilerConfiguration = new ProfilerEnvVarsConfiguration(config);
    when(config.getString(ProfilerEnvVarsConfiguration.CONFIG_KEY_INGEST_URL)).thenReturn(null);
    when(config.getString(eq(ProfilerEnvVarsConfiguration.CONFIG_KEY_OTEL_OTLP_URL), anyString()))
        .thenReturn(endpoint);
    when(config.getString(
            eq(ProfilerEnvVarsConfiguration.CONFIG_KEY_PROFILER_OTLP_PROTOCOL), any()))
        .thenReturn("http/protobuf");

    // when
    String result = profilerConfiguration.getIngestUrl();

    // then
    assertThat(result).isEqualTo(endpoint);
  }

  @Test
  void getIngestUrlSplunkRealm() {
    // given
    ConfigProperties config = mock(ConfigProperties.class);
    ProfilerEnvVarsConfiguration profilerConfiguration = new ProfilerEnvVarsConfiguration(config);
    when(config.getString(ProfilerEnvVarsConfiguration.CONFIG_KEY_INGEST_URL)).thenReturn(null);
    when(config.getString(eq(ProfilerEnvVarsConfiguration.CONFIG_KEY_OTEL_OTLP_URL), anyString()))
        .thenReturn("https://ingest.us0.signalfx.com");
    when(config.getString(
            eq(ProfilerEnvVarsConfiguration.CONFIG_KEY_PROFILER_OTLP_PROTOCOL), any()))
        .thenReturn("http/protobuf");

    // when
    String result = profilerConfiguration.getIngestUrl();

    // then
    assertThat(result).isEqualTo(defaultLogsEndpoint);
  }

  @Test
  void getOtlpProtocolDefault() {
    // given
    ProfilerEnvVarsConfiguration profilerConfiguration =
        new ProfilerEnvVarsConfiguration(
            DefaultConfigProperties.create(Collections.emptyMap(), COMPONENT_LOADER));

    // when
    String result = profilerConfiguration.getOtlpProtocol();

    // then
    assertThat(result).isEqualTo("http/protobuf");
  }

  @Test
  void getOtlpProtocolOtelPropertySet() {
    // given
    ProfilerEnvVarsConfiguration profilerConfiguration =
        new ProfilerEnvVarsConfiguration(
            DefaultConfigProperties.create(
                Collections.singletonMap("otel.exporter.otlp.protocol", "test"), COMPONENT_LOADER));

    // when
    String result = profilerConfiguration.getOtlpProtocol();

    // then
    assertThat(result).isEqualTo("test");
  }

  @Test
  void getOtlpProtocol() {
    // given
    Map<String, String> map = new HashMap<>();
    map.put("otel.exporter.otlp.protocol", "test1");
    map.put("splunk.profiler.otlp.protocol", "test2");

    ProfilerEnvVarsConfiguration profilerConfiguration =
        new ProfilerEnvVarsConfiguration(DefaultConfigProperties.create(map, COMPONENT_LOADER));

    // when
    String result = profilerConfiguration.getOtlpProtocol();

    // then
    assertThat(result).isEqualTo("test2");
  }
}
