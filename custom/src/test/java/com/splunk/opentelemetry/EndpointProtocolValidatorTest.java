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

package com.splunk.opentelemetry;

import static com.splunk.opentelemetry.SplunkConfigurationTest.configuration;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EndpointProtocolValidatorTest {

  @Test
  void testDefaultConfiguration() {
    ConfigProperties config = configuration(Map::of);
    List<String> messages = new ArrayList<>();
    EndpointProtocolValidator.validate(Collections.emptyMap(), config, messages::add);
    assertThat(messages).isEmpty();
  }

  @Test
  void testGrpcEndpoint() {
    ConfigProperties config =
        configuration(() -> Map.of("otel.exporter.otlp.endpoint", "http://localhost:4317"));
    List<String> messages = new ArrayList<>();
    EndpointProtocolValidator.validate(Collections.emptyMap(), config, messages::add);
    assertThat(messages).hasSize(1);
  }

  @Test
  void testGrpcEndpointAndProtocol() {
    ConfigProperties config =
        configuration(
            () ->
                Map.of(
                    "otel.exporter.otlp.endpoint",
                    "http://localhost:4317",
                    "otel.exporter.otlp.protocol",
                    "grpc"));
    List<String> messages = new ArrayList<>();
    EndpointProtocolValidator.validate(Collections.emptyMap(), config, messages::add);
    assertThat(messages).isEmpty();
  }

  @Test
  void testUnknownEndpoint() {
    ConfigProperties config =
        configuration(() -> Map.of("otel.exporter.otlp.endpoint", "http://localhost:5000"));
    List<String> messages = new ArrayList<>();
    EndpointProtocolValidator.validate(Collections.emptyMap(), config, messages::add);
    assertThat(messages).isEmpty();
  }

  @Test
  void testSignalGrpcEndpoint() {
    ConfigProperties config =
        configuration(
            () ->
                Map.of(
                    "otel.exporter.otlp.traces.endpoint",
                    "http://localhost:4317",
                    "otel.exporter.otlp.metrics.endpoint",
                    "http://localhost:4317",
                    "otel.exporter.otlp.logs.endpoint",
                    "http://localhost:4317"));
    List<String> messages = new ArrayList<>();
    EndpointProtocolValidator.validate(Collections.emptyMap(), config, messages::add);
    assertThat(messages).hasSize(3);
  }
}
