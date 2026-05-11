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
import static io.opentelemetry.opamp.client.internal.request.service.HttpRequestService.DEFAULT_DELAY_BETWEEN_REQUESTS;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.YamlDeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.declarativeconfig.internal.model.OpenTelemetryConfigurationModel;
import java.util.Objects;

public class OpampClientConfigurationFactory {
  public static OpampClientConfiguration createConfiguration(
      AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    if (AutoConfigureUtil.isDeclarativeConfig(autoConfiguredOpenTelemetrySdk)) {
      return createConfigurationFromDeclarativeConfig();
    } else {
      return createConfiguration(AutoConfigureUtil.getConfig(autoConfiguredOpenTelemetrySdk));
    }
  }

  private static OpampClientConfiguration createConfigurationFromDeclarativeConfig() {
    OpenTelemetryConfigurationModel configurationModel =
        Objects.requireNonNull(DeclarativeConfigurationInterceptor.getConfigurationModel());

    OpampClientConfiguration.Builder builder = OpampClientConfiguration.builder();
    DeclarativeConfigProperties properties =
        YamlDeclarativeConfigProperties.create(
            configurationModel.getAdditionalProperties(),
            ComponentLoader.forClassLoader(DeclarativeConfigProperties.class.getClassLoader()));

    DeclarativeConfigProperties opampProperties = properties.getStructured("opamp/development");
    if (opampProperties != null) {
      builder.withEnabled(true);
      DeclarativeConfigProperties connection = opampProperties.getStructured("transport", empty());
      DeclarativeConfigProperties http = connection.getStructured("http");
      if (http != null) {
        builder
            .withEndpoint(http.getString("endpoint"))
            .withPollingInterval(
                http.getLong(
                    "polling_interval", DEFAULT_DELAY_BETWEEN_REQUESTS.getNextDelay().toMillis()));
      }
    }

    return builder.build();
  }

  private static OpampClientConfiguration createConfiguration(ConfigProperties config) {
    return OpampClientConfiguration.builder()
        .withEnabled(config.getBoolean("splunk.opamp.enabled", false))
        .withEndpoint(config.getString("splunk.opamp.endpoint"))
        .withPollingInterval(
            config.getLong(
                "splunk.opamp.polling.interval",
                DEFAULT_DELAY_BETWEEN_REQUESTS.getNextDelay().toMillis()))
        .build();
  }
}
