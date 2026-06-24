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

import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProfilerEnvVarsConfigurationFactoryTest {
  private static final ComponentLoader COMPONENT_LOADER =
      ComponentLoader.forClassLoader(
          ProfilerEnvVarsConfigurationFactoryTest.class.getClassLoader());

  String logsEndpoint = "http://logs.example.com";
  String otelEndpoint = "http://otel.example.com";
  String defaultLogsEndpoint = "http://localhost:4318/v1/logs";

  @Test
  void shouldMapPropertiesToConfiguration() {
    ConfigProperties configProperties =
        config(
            Map.ofEntries(
                Map.entry("splunk.profiler.enabled", "true"),
                Map.entry("splunk.profiler.directory", "/tmp/prof"),
                Map.entry("splunk.profiler.recording.duration", "12345ms"),
                Map.entry("splunk.profiler.keep-files", "true"),
                Map.entry("splunk.profiler.logs-endpoint", logsEndpoint),
                Map.entry("splunk.profiler.otlp.protocol", "grpc"),
                Map.entry("splunk.profiler.memory.enabled", "true"),
                Map.entry("splunk.profiler.memory.event.rate-limit.enabled", "true"),
                Map.entry("splunk.profiler.memory.event.rate", "250/s"),
                Map.entry("splunk.profiler.memory.native.sampling", "true"),
                Map.entry("splunk.profiler.call.stack.interval", "1410ms"),
                Map.entry("splunk.profiler.include.agent.internals", "true"),
                Map.entry("splunk.profiler.include.jvm.internals", "true"),
                Map.entry("splunk.profiler.tracing.stacks.only", "true"),
                Map.entry("splunk.profiler.max.stack.depth", "73")));

    ProfilerConfiguration profilerConfiguration =
        ProfilerEnvVarsConfigurationFactory.create(configProperties);

    assertThat(profilerConfiguration.isEnabled()).isTrue();
    assertThat(profilerConfiguration.getIngestUrl()).isEqualTo(logsEndpoint);
    assertThat(profilerConfiguration.getOtlpProtocol()).isEqualTo("grpc");
    assertThat(profilerConfiguration.getMemoryEnabled()).isTrue();
    assertThat(profilerConfiguration.getMemoryEventRateLimitEnabled()).isTrue();
    assertThat(profilerConfiguration.getMemoryEventRate()).isEqualTo("250/s");
    assertThat(profilerConfiguration.getUseAllocationSampleEvent())
        .isEqualTo(ProfilerConfiguration.HAS_OBJECT_ALLOCATION_SAMPLE_EVENT);
    assertThat(profilerConfiguration.getCallStackInterval()).isEqualTo(Duration.ofMillis(1410));
    assertThat(profilerConfiguration.getIncludeAgentInternalStacks()).isTrue();
    assertThat(profilerConfiguration.getIncludeJvmInternalStacks()).isTrue();
    assertThat(profilerConfiguration.getTracingStacksOnly()).isTrue();
    assertThat(profilerConfiguration.getStackDepth()).isEqualTo(73);
    assertThat(profilerConfiguration.getKeepFiles()).isTrue();
    assertThat(profilerConfiguration.getProfilerDirectory()).isEqualTo("/tmp/prof");
    assertThat(profilerConfiguration.getRecordingDuration()).isEqualTo(Duration.ofMillis(12345));
    assertThat(profilerConfiguration.getConfigProperties()).isSameAs(configProperties);
  }

  @Test
  void getIngestUrl_endpointDefined() {
    ProfilerConfiguration profilerConfiguration =
        ProfilerEnvVarsConfigurationFactory.create(
            config(
                Map.of(ProfilerEnvVarsConfigurationFactory.CONFIG_KEY_INGEST_URL, logsEndpoint)));

    assertThat(profilerConfiguration.getIngestUrl()).isEqualTo(logsEndpoint);
  }

  @Test
  void getIngestUrl_endpointNotDefined_usedOtelGrpc() {
    ProfilerConfiguration profilerConfiguration =
        ProfilerEnvVarsConfigurationFactory.create(
            config(
                Map.of(
                    ProfilerEnvVarsConfigurationFactory.CONFIG_KEY_OTEL_OTLP_URL,
                    otelEndpoint,
                    ProfilerEnvVarsConfigurationFactory.CONFIG_KEY_PROFILER_OTLP_PROTOCOL,
                    "grpc")));

    assertThat(profilerConfiguration.getIngestUrl()).isEqualTo(otelEndpoint);
  }

  @Test
  void getIngestUrl_endpointNotDefined_usedOtelHttpProtobuf() {
    ProfilerConfiguration profilerConfiguration =
        ProfilerEnvVarsConfigurationFactory.create(
            config(
                Map.of(
                    ProfilerEnvVarsConfigurationFactory.CONFIG_KEY_OTEL_OTLP_URL,
                    otelEndpoint,
                    ProfilerEnvVarsConfigurationFactory.CONFIG_KEY_PROFILER_OTLP_PROTOCOL,
                    "http/protobuf")));

    assertThat(profilerConfiguration.getIngestUrl()).isEqualTo(otelEndpoint + "/v1/logs");
  }

  @Test
  void getIngestUrl_endpointNotDefined_usedOtelHttpProtobufWithPath() {
    String endpoint = otelEndpoint + "/v1/logs";

    ProfilerConfiguration profilerConfiguration =
        ProfilerEnvVarsConfigurationFactory.create(
            config(
                Map.of(
                    ProfilerEnvVarsConfigurationFactory.CONFIG_KEY_OTEL_OTLP_URL,
                    endpoint,
                    ProfilerEnvVarsConfigurationFactory.CONFIG_KEY_PROFILER_OTLP_PROTOCOL,
                    "http/protobuf")));

    assertThat(profilerConfiguration.getIngestUrl()).isEqualTo(endpoint);
  }

  @Test
  void getIngestUrlSplunkRealm() {
    ProfilerConfiguration profilerConfiguration =
        ProfilerEnvVarsConfigurationFactory.create(
            config(
                Map.of(
                    ProfilerEnvVarsConfigurationFactory.CONFIG_KEY_OTEL_OTLP_URL,
                    "https://ingest.us0.observability.splunkcloud.com",
                    ProfilerEnvVarsConfigurationFactory.CONFIG_KEY_PROFILER_OTLP_PROTOCOL,
                    "http/protobuf")));

    assertThat(profilerConfiguration.getIngestUrl()).isEqualTo(defaultLogsEndpoint);
  }

  @Test
  void getOtlpProtocolDefault() {
    ProfilerConfiguration profilerConfiguration =
        ProfilerEnvVarsConfigurationFactory.create(config(Collections.emptyMap()));

    assertThat(profilerConfiguration.getOtlpProtocol()).isEqualTo("http/protobuf");
  }

  @Test
  void getOtlpProtocolOtelPropertySet() {
    ProfilerConfiguration profilerConfiguration =
        ProfilerEnvVarsConfigurationFactory.create(
            config(Collections.singletonMap("otel.exporter.otlp.protocol", "test")));

    assertThat(profilerConfiguration.getOtlpProtocol()).isEqualTo("test");
  }

  @Test
  void getOtlpProtocol() {
    Map<String, String> map = new HashMap<>();
    map.put("otel.exporter.otlp.protocol", "test1");
    map.put("splunk.profiler.otlp.protocol", "test2");

    ProfilerConfiguration profilerConfiguration =
        ProfilerEnvVarsConfigurationFactory.create(config(map));

    assertThat(profilerConfiguration.getOtlpProtocol()).isEqualTo("test2");
  }

  private static ConfigProperties config(Map<String, String> map) {
    return DefaultConfigProperties.create(map, COMPONENT_LOADER);
  }
}
