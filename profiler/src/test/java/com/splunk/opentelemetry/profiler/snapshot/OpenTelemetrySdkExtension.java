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

package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class OpenTelemetrySdkExtension implements AfterEachCallback, OpenTelemetry {
  public static Builder builder() {
    return new Builder();
  }

  private final OpenTelemetrySdk sdk;

  private OpenTelemetrySdkExtension(OpenTelemetrySdk sdk) {
    this.sdk = sdk;
  }

  @Override
  public TracerProvider getTracerProvider() {
    return sdk.getTracerProvider();
  }

  @Override
  public MeterProvider getMeterProvider() {
    return sdk.getMeterProvider();
  }

  @Override
  public LoggerProvider getLogsBridge() {
    return sdk.getLogsBridge();
  }

  @Override
  public ContextPropagators getPropagators() {
    return sdk.getPropagators();
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) {
    sdk.close();
  }

  /**
   * An extremely simplified adaptation of the OpenTelemetry class
   * AutoConfiguredOpenTelemetrySdkBuilder, designed explicitly to facilitate easier component-like
   * testing of custom OpenTelemetry Java Agent extensions.
   */
  public static class Builder {
    private final SdkCustomizer customizer = new SdkCustomizer();
    private final Map<String, String> properties = new HashMap<>();

    public Builder withProperty(String name, String value) {
      properties.put(name, value);
      return this;
    }

    public Builder with(AutoConfigurationCustomizerProvider provider) {
      provider.customize(customizer);
      return this;
    }

    /**
     * Simplified re-implementation of AutoConfiguredOpenTelemetrySdkBuilder's build method. The
     * OpenTelemetry SDK is only configured with features necessary to pass existing test use cases.
     */
    public OpenTelemetrySdkExtension build() {
      overrideProperties();

      OpenTelemetrySdkBuilder sdkBuilder = OpenTelemetrySdk.builder();
      OpenTelemetrySdk sdk = sdkBuilder.build();

      return new OpenTelemetrySdkExtension(sdk);
    }

    private void overrideProperties() {
      var properties = DefaultConfigProperties.createFromMap(this.properties);
      for (var customizer : customizer.propertyCustomizers) {
        var overrides = customizer.apply(properties);
        properties = properties.withOverrides(overrides);
      }
    }
  }

  private static class SdkCustomizer implements AutoConfigurationCustomizer {
    private final List<Function<ConfigProperties, Map<String, String>>> propertyCustomizers =
        new ArrayList<>();

    @Override
    public AutoConfigurationCustomizer addTracerProviderCustomizer(
        BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder>
            tracerProviderCustomizer) {
      return this;
    }

    @Override
    public AutoConfigurationCustomizer addPropagatorCustomizer(
        BiFunction<? super TextMapPropagator, ConfigProperties, ? extends TextMapPropagator>
            textMapPropagator) {
      return this;
    }

    @Override
    public AutoConfigurationCustomizer addPropertiesCustomizer(
        Function<ConfigProperties, Map<String, String>> propertiesCustomizer) {
      this.propertyCustomizers.add(propertiesCustomizer);
      return this;
    }

    @Override
    public AutoConfigurationCustomizer addResourceCustomizer(
        BiFunction<? super Resource, ConfigProperties, ? extends Resource> biFunction) {
      return this;
    }

    @Override
    public AutoConfigurationCustomizer addSamplerCustomizer(
        BiFunction<? super Sampler, ConfigProperties, ? extends Sampler> biFunction) {
      return null;
    }

    @Override
    public AutoConfigurationCustomizer addSpanExporterCustomizer(
        BiFunction<? super SpanExporter, ConfigProperties, ? extends SpanExporter> biFunction) {
      return this;
    }

    @Override
    public AutoConfigurationCustomizer addPropertiesSupplier(
        Supplier<Map<String, String>> supplier) {
      return this;
    }
  }
}
