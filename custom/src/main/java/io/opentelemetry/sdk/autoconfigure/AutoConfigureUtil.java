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

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import java.lang.reflect.Field;

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
    return sdk.getConfig();
  }

  public static Resource getResource(AutoConfiguredOpenTelemetrySdk sdk) {
    Resource resource = sdk.getResource();

    //    if (resource.equals(Resource.getDefault())) {
    //      resource = extractResource(sdk);
    //    }

    return resource;
  }

  // This is a workaround for the issue with resource not being correctly initialized.
  // Currently, AutoConfiguredOpenTelemetrySdk gets a default Resource when created, but providers
  // that were created based on the model get correctly configured instance of the Resource.
  // See: https://github.com/open-telemetry/opentelemetry-java/pull/7418 for potential fix of the
  // issue.
  // TODO: This method should be removed once the fix (or similar) is merged in the upstream.
  private static Resource extractResource(AutoConfiguredOpenTelemetrySdk sdk) {
    Resource resource =
        extractResourceFromProvider(sdk.getOpenTelemetrySdk().getSdkTracerProvider());
    if (resource == null || resource.equals(Resource.getDefault())) {
      resource = extractResourceFromProvider(sdk.getOpenTelemetrySdk().getSdkMeterProvider());
    }
    if (resource == null || resource.equals(Resource.getDefault())) {
      resource = extractResourceFromProvider(sdk.getOpenTelemetrySdk().getSdkLoggerProvider());
    }
    if (resource == null) {
      resource = Resource.getDefault();
    }

    return resource;
  }

  private static Resource extractResourceFromProvider(Object provider) {
    Field sharedStateField = null;
    try {
      sharedStateField = provider.getClass().getDeclaredField("sharedState");
      sharedStateField.setAccessible(true);
      Object sharedState = sharedStateField.get(provider);

      Field resourceField = sharedState.getClass().getDeclaredField("resource");
      resourceField.setAccessible(true);
      return (Resource) resourceField.get(sharedState);
    } catch (Exception e) {
      return null;
    }
  }
}
