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
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class SnapshotProfilingSdkCustomizer implements AutoConfigurationCustomizerProvider {
  private final TraceRegistry registry;
  private final Function<ConfigProperties, StackTraceSampler> samplerProvider;
  private final ContextStorageWrapper contextStorageWrapper;

  public SnapshotProfilingSdkCustomizer() {
    this(
        TraceRegistryHolder.getTraceRegistry(),
        stackTraceSamplerProvider(),
        new ContextStorageWrapper());
  }

  private static Function<ConfigProperties, StackTraceSampler> stackTraceSamplerProvider() {
    return properties -> {
      SnapshotProfilingEnvVarsConfiguration configuration =
          new SnapshotProfilingEnvVarsConfiguration(properties);
      Duration samplingPeriod = configuration.getSamplingInterval();
      StagingArea.SUPPLIER.configure(createStagingArea(configuration));
      return new PeriodicStackTraceSampler(
          StagingArea.SUPPLIER, SpanTracker.SUPPLIER, samplingPeriod);
    };
  }

  private static StagingArea createStagingArea(
      SnapshotProfilingEnvVarsConfiguration configuration) {
    Duration interval = configuration.getExportInterval();
    int capacity = configuration.getStagingCapacity();
    return new PeriodicallyExportingStagingArea(StackTraceExporter.SUPPLIER, interval, capacity);
  }

  @VisibleForTesting
  SnapshotProfilingSdkCustomizer(
      TraceRegistry registry,
      StackTraceSampler sampler,
      ContextStorageWrapper contextStorageWrapper) {
    this(registry, properties -> sampler, contextStorageWrapper);
  }

  private SnapshotProfilingSdkCustomizer(
      TraceRegistry registry,
      Function<ConfigProperties, StackTraceSampler> samplerProvider,
      ContextStorageWrapper contextStorageWrapper) {
    this.registry = registry;
    this.samplerProvider = samplerProvider;
    this.contextStorageWrapper = contextStorageWrapper;
  }

  @Override
  public void customize(AutoConfigurationCustomizer autoConfigurationCustomizer) {
    autoConfigurationCustomizer
        .addTracerProviderCustomizer(snapshotProfilingSpanProcessor(registry))
        .addPropertiesCustomizer(setupStackTraceSampler())
        .addPropertiesCustomizer(startTrackingActiveSpans(registry))
        .addTracerProviderCustomizer(addShutdownHook());
  }

  private BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder>
      addShutdownHook() {
    return (builder, properties) -> {
      if (snapshotProfilingEnabled(properties)) {
        builder.addSpanProcessor(new SdkShutdownHook());
      }
      return builder;
    };
  }

  private BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder>
      snapshotProfilingSpanProcessor(TraceRegistry registry) {
    return (builder, properties) -> {
      if (snapshotProfilingEnabled(properties)) {
        double selectionProbability =
            new SnapshotProfilingEnvVarsConfiguration(properties).getSnapshotSelectionProbability();

        return builder.addSpanProcessor(
            new SnapshotProfilingSpanProcessor(
                registry, new TraceIdBasedSnapshotSelector(selectionProbability)));
      }
      return builder;
    };
  }

  private boolean includeTraceContextPropagator(Set<String> configuredPropagators) {
    return configuredPropagators.isEmpty();
  }

  private Function<ConfigProperties, Map<String, String>> setupStackTraceSampler() {
    return properties -> {
      if (snapshotProfilingEnabled(properties)) {
        StackTraceSampler sampler = samplerProvider.apply(properties);
        ConfigurableSupplier<StackTraceSampler> supplier = StackTraceSampler.SUPPLIER;
        supplier.configure(sampler);
      }
      return Collections.emptyMap();
    };
  }

  private Function<ConfigProperties, Map<String, String>> startTrackingActiveSpans(
      TraceRegistry registry) {
    return properties -> {
      if (snapshotProfilingEnabled(properties)) {
        contextStorageWrapper.wrapContextStorage(registry);
      }
      return Collections.emptyMap();
    };
  }

  private boolean snapshotProfilingEnabled(ConfigProperties properties) {
    return new SnapshotProfilingEnvVarsConfiguration(properties).isEnabled();
  }
}
