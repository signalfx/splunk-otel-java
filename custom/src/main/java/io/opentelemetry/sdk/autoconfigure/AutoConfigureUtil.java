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

import io.opentelemetry.api.incubator.config.ConfigProvider;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;

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

  /** Returns the {@link ConfigProperties} used for auto-configuration. */
  public static ConfigProperties getConfig(AutoConfiguredOpenTelemetrySdk sdk) {
    return resolveConfigProperties(sdk);
  }

  public static Resource getResource(AutoConfiguredOpenTelemetrySdk sdk) {
    return sdk.getResource();
  }

  /** Resolve {@link ConfigProperties} from the {@code autoConfiguredOpenTelemetrySdk}. */
  static ConfigProperties resolveConfigProperties(
      AutoConfiguredOpenTelemetrySdk sdk) {
    ConfigProperties sdkConfigProperties =
        io.opentelemetry.sdk.autoconfigure.internal.AutoConfigureUtil.getConfig(sdk);
    if (sdkConfigProperties != null) {
      return sdkConfigProperties;
    }
    ConfigProvider configProvider =
        io.opentelemetry.sdk.autoconfigure.internal.AutoConfigureUtil.getConfigProvider(sdk);
    if (configProvider != null) {
      DeclarativeConfigProperties instrumentationConfig = configProvider.getInstrumentationConfig();

      if (instrumentationConfig != null) {
        return new TmpDeclarativeConfigPropertiesBridge(instrumentationConfig);
      }
    }
    // Should never happen
    throw new IllegalStateException(
        "AutoConfiguredOpenTelemetrySdk does not have ConfigProperties or DeclarativeConfigProperties. This is likely a programming error in opentelemetry-java");
  }
}
