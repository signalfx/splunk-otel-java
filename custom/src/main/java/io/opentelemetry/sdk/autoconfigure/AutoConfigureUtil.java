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
import io.opentelemetry.instrumentation.config.bridge.ConfigPropertiesBackedConfigProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import java.util.logging.Logger;
import javax.annotation.Nullable;

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
  private static final Logger logger = Logger.getLogger(AutoConfigureUtil.class.getName());

  private AutoConfigureUtil() {}

  /** Returns the {@link ConfigProperties} used for auto-configuration. */
  public static ConfigProperties getConfig(AutoConfiguredOpenTelemetrySdk sdk) {
    return sdk.getConfig();
  }

  public static boolean isDeclarativeConfig(AutoConfiguredOpenTelemetrySdk sdk) {
    OpenTelemetry openTelemetry = sdk.getOpenTelemetrySdk();
    if (openTelemetry instanceof ExtendedOpenTelemetry) {
      return !(((ExtendedOpenTelemetry) openTelemetry).getConfigProvider()
          instanceof ConfigPropertiesBackedConfigProvider);
    }
    return false;
  }

  @Nullable
  public static DeclarativeConfigProperties getDistributionConfig(
      AutoConfiguredOpenTelemetrySdk sdk) {
    OpenTelemetry openTelemetry = sdk.getOpenTelemetrySdk();
    if (!(openTelemetry instanceof ExtendedOpenTelemetry)) {
      return null;
    }
    ConfigProvider configProvider = ((ExtendedOpenTelemetry) openTelemetry).getConfigProvider();
    if (configProvider == null) {
      return null;
    }

    // TODO: This is temporary solution until distribution config support is implemented in the
    //       upstream. For now assume that distribution node is located under
    //       .instrumentation/development.java.distribution
    //       Replace this code with `return sdk.getConfigProvider().getDistributionConfig()` once is
    //       implemented
    DeclarativeConfigProperties instrumentationConfig = configProvider.getInstrumentationConfig();
    if (instrumentationConfig == null) {
      return null;
    }
    return instrumentationConfig
        .getStructured("java", empty())
        .getStructured("distribution", empty());
  }

  public static Resource getResource(AutoConfiguredOpenTelemetrySdk sdk) {
    return sdk.getResource();
  }
}
