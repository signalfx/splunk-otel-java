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

import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.instrumentation.config.bridge.DeclarativeConfigPropertiesBridgeBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.SdkConfigProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.TracerProviderModel;
import java.time.Duration;
import java.util.function.Function;

@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class SnapshotProfilingConfigurationCustomizerProvider
    implements DeclarativeConfigurationCustomizerProvider {
  private final TraceRegistry registry;
  private final Function<ConfigProperties, StackTraceSampler> samplerProvider;
  private final ContextStorageWrapper contextStorageWrapper;

  public SnapshotProfilingConfigurationCustomizerProvider() {
    this(
        TraceRegistryHolder.getTraceRegistry(),
        stackTraceSamplerProvider(),
        new ContextStorageWrapper());
  }

  public void customize(DeclarativeConfigurationCustomizer configurationCustomizer) {
    configurationCustomizer.addModelCustomizer(this::customizeModel);
  }

  @VisibleForTesting
  OpenTelemetryConfigurationModel customizeModel(OpenTelemetryConfigurationModel model) {
    if (isSnapshotProfilingEnabled(model)) {
      setupStackTraceSampler(model);
      addShutdownHookSpanProcessor(model);
      initActiveSpansTracking();
    }
    return model;
  }

  private void setupStackTraceSampler(OpenTelemetryConfigurationModel model) {
    DeclarativeConfigProperties instrumentationConfig =
        SdkConfigProvider.create(model).getInstrumentationConfig();
    ConfigProperties properties =
        new DeclarativeConfigPropertiesBridgeBuilder()
            .buildFromInstrumentationConfig(instrumentationConfig);
    StackTraceSampler sampler = samplerProvider.apply(properties);
    ConfigurableSupplier<StackTraceSampler> supplier = StackTraceSampler.SUPPLIER;
    supplier.configure(sampler);
  }

  private void addShutdownHookSpanProcessor(OpenTelemetryConfigurationModel model) {
    if (model.getTracerProvider() == null) {
      model.withTracerProvider(new TracerProviderModel());
    }
    model
        .getTracerProvider()
        .getProcessors()
        .add(
            new SpanProcessorModel()
                .withAdditionalProperty(SdkShutdownHookComponentProvider.NAME, null));
  }

  private void initActiveSpansTracking() {
    contextStorageWrapper.wrapContextStorage(registry);
  }

  private static Function<ConfigProperties, StackTraceSampler> stackTraceSamplerProvider() {
    return properties -> {
      Duration samplingPeriod = SnapshotProfilingConfiguration.getSamplingInterval(properties);
      StagingArea.SUPPLIER.configure(createStagingArea(properties));
      return new PeriodicStackTraceSampler(
          StagingArea.SUPPLIER, SpanTracker.SUPPLIER, samplingPeriod);
    };
  }

  private static StagingArea createStagingArea(ConfigProperties properties) {
    Duration interval = SnapshotProfilingConfiguration.getExportInterval(properties);
    int capacity = SnapshotProfilingConfiguration.getStagingCapacity(properties);
    return new PeriodicallyExportingStagingArea(StackTraceExporter.SUPPLIER, interval, capacity);
  }

  private static boolean isSnapshotProfilingEnabled(OpenTelemetryConfigurationModel model) {
    SdkConfigProvider configProvider = SdkConfigProvider.create(model);
    if (configProvider.getInstrumentationConfig() == null) {
      return false;
    }
    return configProvider
        .getInstrumentationConfig()
        .getStructured("java", DeclarativeConfigProperties.empty())
        .getStructured("splunk", DeclarativeConfigProperties.empty())
        .getStructured("snapshot", DeclarativeConfigProperties.empty())
        .getStructured("profiler", DeclarativeConfigProperties.empty())
        .getBoolean("enabled", false);
  }

  @VisibleForTesting
  SnapshotProfilingConfigurationCustomizerProvider(
      TraceRegistry registry,
      StackTraceSampler sampler,
      ContextStorageWrapper contextStorageWrapper) {
    this(registry, properties -> sampler, contextStorageWrapper);
  }

  private SnapshotProfilingConfigurationCustomizerProvider(
      TraceRegistry registry,
      Function<ConfigProperties, StackTraceSampler> samplerProvider,
      ContextStorageWrapper contextStorageWrapper) {
    this.registry = registry;
    this.samplerProvider = samplerProvider;
    this.contextStorageWrapper = contextStorageWrapper;
  }
}
