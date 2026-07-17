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

import static io.opentelemetry.opamp.client.internal.request.service.HttpRequestService.DEFAULT_DELAY_BETWEEN_REQUESTS;
import static org.assertj.core.api.Assertions.assertThat;

import com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

class OpampClientConfigurationFactoryTest {
  @RegisterExtension static final AutoCleanupExtension autoCleanup = AutoCleanupExtension.create();

  @AfterEach
  void afterEach() {
    DeclarativeConfigurationInterceptor.reset();
  }

  @Test
  void shouldCreateConfigurationFromEnvVars() {
    // given
    AutoConfiguredOpenTelemetrySdk sdk =
        createSdk(
            Map.of(
                "splunk.opamp.enabled", "true",
                "splunk.opamp.endpoint", "https://opamp.example.com",
                "splunk.opamp.polling.interval", "3210",
                "splunk.opamp.experimental.remote.control", "true"));

    // when
    OpampClientConfiguration configuration =
        OpampClientConfigurationFactory.createConfiguration(sdk);

    // then
    assertThat(configuration.isEnabled()).isTrue();
    assertThat(configuration.getEndpoint()).isEqualTo("https://opamp.example.com");
    assertThat(configuration.getPollingInterval()).isEqualTo(3210);
    assertThat(configuration.remoteControlIsAllowed()).isTrue();
  }

  @Test
  void shouldCreateConfigurationFromDeclarativeConfig(@TempDir Path tempDir) throws IOException {
    // given
    String yaml =
        """
            file_format: "1.0"
            distribution:
              splunk:
                opamp/development:
                  endpoint: http://some.opamp-host.com:3420/v1/opamp
                  polling_interval: 4567
            """;
    AutoConfiguredOpenTelemetrySdk sdk =
        DeclarativeConfigTestUtil.createAutoConfiguredSdk(yaml, tempDir, autoCleanup);

    // when
    OpampClientConfiguration configuration =
        OpampClientConfigurationFactory.createConfiguration(sdk);

    // then
    assertThat(configuration.isEnabled()).isTrue();
    assertThat(configuration.getEndpoint()).isEqualTo("http://some.opamp-host.com:3420/v1/opamp");
    assertThat(configuration.getPollingInterval()).isEqualTo(4567);
    assertThat(configuration.remoteControlIsAllowed()).isFalse();
  }

  @Test
  void shouldEnableRemoteControlFromDeclarativeConfig(@TempDir Path tempDir) throws IOException {
    // given
    String yaml =
        """
            file_format: "1.0"
            distribution:
              splunk:
                opamp/development:
                  endpoint: http://some.opamp-host.com:3420/v1/opamp
                  features:
                    experimental_control:
            """;
    AutoConfiguredOpenTelemetrySdk sdk =
        DeclarativeConfigTestUtil.createAutoConfiguredSdk(yaml, tempDir, autoCleanup);

    // when
    OpampClientConfiguration configuration =
        OpampClientConfigurationFactory.createConfiguration(sdk);

    // then
    assertThat(configuration.remoteControlIsAllowed()).isTrue();
  }

  @Test
  void shouldUseDefaults() {
    // given
    AutoConfiguredOpenTelemetrySdk sdk = createSdk(Map.of());

    // when
    OpampClientConfiguration configuration =
        OpampClientConfigurationFactory.createConfiguration(sdk);

    // then
    assertThat(configuration.isEnabled()).isFalse();
    assertThat(configuration.getEndpoint()).isNull();
    assertThat(configuration.getPollingInterval())
        .isEqualTo(DEFAULT_DELAY_BETWEEN_REQUESTS.getNextDelay().toMillis());
  }

  private static AutoConfiguredOpenTelemetrySdk createSdk(Map<String, String> properties) {
    AutoConfiguredOpenTelemetrySdk sdk =
        AutoConfiguredOpenTelemetrySdk.builder().addPropertiesSupplier(() -> properties).build();
    autoCleanup.deferCleanup(sdk.getOpenTelemetrySdk());
    return sdk;
  }
}
