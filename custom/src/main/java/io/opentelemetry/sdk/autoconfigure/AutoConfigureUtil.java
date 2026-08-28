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

package io.opentelemetry.sdk.autoconfigure;

import static io.opentelemetry.api.incubator.config.DeclarativeConfigProperties.empty;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.incubator.ExtendedOpenTelemetry;
import io.opentelemetry.api.incubator.config.ConfigProvider;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.YamlDeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.DistributionModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.DistributionPropertyModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class is a hack to allows us to call getResource() and getConfig() on the
 * AutoConfiguredOpenTelemetrySdk. This is merely here as a stop-gap measure until other means are
 * in place.
 *
 * <p>See the discussion here:
 * https://github.com/open-telemetry/opentelemetry-java/pull/5467#discussion_r1239559127
 *
 * <p>This class is internal and is not intended for public use.
 */
public final class AutoConfigureUtil {
  private AutoConfigureUtil() {}

  /** Returns the {@link ConfigProperties} used for non-declarative autoconfiguration. */
  public static ConfigProperties getConfig(AutoConfiguredOpenTelemetrySdk sdk) {
    return Objects.requireNonNull(
        io.opentelemetry.sdk.autoconfigure.internal.AutoConfigureUtil.getConfig(sdk),
        "ConfigProperties are unavailable when declarative configuration is used");
  }

  public static boolean isDeclarativeConfig(AutoConfiguredOpenTelemetrySdk sdk) {
    return io.opentelemetry.sdk.autoconfigure.internal.AutoConfigureUtil.getConfig(sdk) == null;
  }

  public static ConfigProvider getConfigProvider(AutoConfiguredOpenTelemetrySdk sdk) {
    OpenTelemetry openTelemetry = sdk.getOpenTelemetrySdk();
    if (!(openTelemetry instanceof ExtendedOpenTelemetry)) {
      throw new IllegalStateException(
          "ConfigProvider is unavailable because declarative configuration is not in use");
    }

    return Objects.requireNonNull(
        ((ExtendedOpenTelemetry) openTelemetry).getConfigProvider(),
        "ConfigProvider is unavailable");
  }

  public static DeclarativeConfigProperties getDistributionConfig(
      OpenTelemetryConfigurationModel model) {
    DistributionModel distributionModel = model.getDistribution();
    if (distributionModel == null) {
      return empty();
    }

    DistributionPropertyModel splunkModel =
        distributionModel.getAdditionalProperties().get("splunk");
    if (splunkModel == null) {
      return empty();
    }

    ComponentLoader componentLoader =
        ComponentLoader.forClassLoader(AutoConfigureUtil.class.getClassLoader());
    return YamlDeclarativeConfigProperties.create(
        splunkModel.getAdditionalProperties(), componentLoader);
  }

  public static DeclarativeConfigProperties getInstrumentationConfig(
      OpenTelemetryConfigurationModel model) {
    if (model.getInstrumentationDevelopment() == null
        || model.getInstrumentationDevelopment().getJava() == null) {
      return empty();
    }

    Map<String, Object> properties =
        model
            .getInstrumentationDevelopment()
            .getJava()
            .getAdditionalProperties()
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, v -> v.getValue().getAdditionalProperties()));

    ComponentLoader componentLoader =
        ComponentLoader.forClassLoader(AutoConfigureUtil.class.getClassLoader());

    return YamlDeclarativeConfigProperties.create(properties, componentLoader);
  }

  public static Resource getResource(AutoConfiguredOpenTelemetrySdk sdk) {
    return sdk.getResource();
  }
}
