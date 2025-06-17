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

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.internal.SpiHelper;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class SplunkDeclarativeConfigurationTest {
  @Test
  void shouldCustomizeConfigPropertiesIfUndefined(@TempDir Path tempDir) throws IOException {
    String yaml =
        """
            file_format: "0.4"
            instrumentation/development:
              java:
            """
        ;

    AutoConfiguredOpenTelemetrySdk sdk = createConfigFile(yaml, tempDir);
    ConfigProperties configProperties = AutoConfigureUtil.getConfig(sdk);
    assertThat(configProperties.getBoolean("splunk.metrics.force_full_commandline")).isFalse();
    assertThat(configProperties.getBoolean("otel.instrumentation.spring-batch.enabled")).isTrue();
    assertThat(configProperties.getBoolean("otel.instrumentation.spring-batch.item.enabled")).isTrue();
  }

  @Test
  void shouldKeepOriginalConfigProperties(@TempDir Path tempDir) throws IOException {
    String yaml =
        """
            file_format: "0.4"
            instrumentation/development:
              java:
                spring-batch:
                  enabled: false
                  item:
                    enabled: false
                splunk:
                  metrics:
                    force_full_commandline: true
            """
        ;

    AutoConfiguredOpenTelemetrySdk sdk = createConfigFile(yaml, tempDir);
    ConfigProperties configProperties = AutoConfigureUtil.getConfig(sdk);
    assertThat(configProperties.getBoolean("splunk.metrics.force_full_commandline")).isTrue();
    assertThat(configProperties.getBoolean("otel.instrumentation.spring-batch.enabled")).isFalse();
    assertThat(configProperties.getBoolean("otel.instrumentation.spring-batch.item.enabled")).isFalse();
  }

  @Test
  void shouldCustomizeSamplerIfUndefined() {
    String yaml =
        """
            file_format: "0.4"
            tracer_provider:
              processors:
                - batch:
                    exporter:
                      console:
              sampler:
            instrumentation/development:
              java:
            """
            ;

    OpenTelemetryConfigurationModel configurationModel = DeclarativeConfiguration.parse(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    OpenTelemetrySdk sdk = DeclarativeConfiguration.create(configurationModel, SpiHelper.serviceComponentLoader(
        SplunkDeclarativeConfigurationTest.class.getClassLoader()));

    assertThat(sdk.getSdkTracerProvider().getSampler().getDescription()).isEqualTo("AlwaysOnSampler");
  }
  @Test
  void shouldKeepOriginalSamplerConfiguration() {
    String yaml =
        """
            file_format: "0.4"
            tracer_provider:
              processors:
                - batch:
                    exporter:
                      console:
              sampler:
                always_off:
            instrumentation/development:
              java:
            """
        ;

    OpenTelemetryConfigurationModel configurationModel = DeclarativeConfiguration.parse(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    OpenTelemetrySdk sdk = DeclarativeConfiguration.create(configurationModel, SpiHelper.serviceComponentLoader(
        SplunkDeclarativeConfigurationTest.class.getClassLoader()));

    assertThat(sdk.getSdkTracerProvider().getSampler().getDescription()).isEqualTo("AlwaysOffSampler");
  }

  private static AutoConfiguredOpenTelemetrySdk createConfigFile(String yaml, Path tempDir) throws IOException {
    Path configFilePath = tempDir.resolve("test-config.yaml");
    Files.writeString(configFilePath, yaml);
    System.setProperty("otel.experimental.config.file", configFilePath.toString());

    return AutoConfiguredOpenTelemetrySdk.builder().build();
  }
}
