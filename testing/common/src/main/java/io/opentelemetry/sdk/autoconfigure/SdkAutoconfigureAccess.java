package io.opentelemetry.sdk.autoconfigure;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;

/**
 * This is a copy of utility class taken from opentelemetry-javaagent-tooling.
 */
public final class SdkAutoconfigureAccess {
  private SdkAutoconfigureAccess() {}

  public static Resource getResource(AutoConfiguredOpenTelemetrySdk sdk) {
    return sdk.getResource();
  }

  public static AutoConfiguredOpenTelemetrySdk create(
      OpenTelemetrySdk sdk, Resource resource, ConfigProperties config, Object configProvider) {
    return AutoConfiguredOpenTelemetrySdk.create(sdk, resource, config, configProvider);
  }
}
