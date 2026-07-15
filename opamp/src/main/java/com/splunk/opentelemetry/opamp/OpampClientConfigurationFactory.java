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

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
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
    OpenTelemetryConfigurationModel config =
        Objects.requireNonNull(DeclarativeConfigurationInterceptor.getConfigurationModel());

    DeclarativeConfigProperties opampProperties =
        AutoConfigureUtil.getDistributionConfig(config).getStructured("opamp/development");

    OpampClientConfiguration.Builder builder = OpampClientConfiguration.builder();
    if (opampProperties != null) {
      builder.withEnabled(true);
      builder
          .withEndpoint(opampProperties.getString("endpoint"))
          .withPollingInterval(
              opampProperties.getLong(
                  "polling_interval", DEFAULT_DELAY_BETWEEN_REQUESTS.getNextDelay().toMillis()))
          .withHackyRemoteControl(opampProperties.getBoolean("experimental_control"));
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
        .withHackyRemoteControl(config.getBoolean("splunk.opamp.experimental_remote_control", false))
        .build();
  }
}
