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
import java.util.Collections;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class SnapshotProfilingSdkCustomizer implements AutoConfigurationCustomizerProvider {
  private final TraceRegistry registry;
  private final ContextStorageWrapper contextStorageWrapper;

  public SnapshotProfilingSdkCustomizer() {
    this(TraceRegistryHolder.getTraceRegistry(), new ContextStorageWrapper());
  }

  @VisibleForTesting
  SnapshotProfilingSdkCustomizer(
      TraceRegistry registry, ContextStorageWrapper contextStorageWrapper) {
    this.registry = registry;
    this.contextStorageWrapper = contextStorageWrapper;
  }

  @Override
  public void customize(AutoConfigurationCustomizer autoConfigurationCustomizer) {
    // initializeSnapshotProfilingConfiguration must be executed first
    autoConfigurationCustomizer.addPropertiesCustomizer(initializeSnapshotProfilingConfiguration());

    autoConfigurationCustomizer
        .addTracerProviderCustomizer(snapshotProfilingSpanProcessor(registry))
        .addPropertiesCustomizer(startTrackingActiveSpans(registry))
        .addTracerProviderCustomizer(addShutdownHook());
  }

  private Function<ConfigProperties, Map<String, String>>
      initializeSnapshotProfilingConfiguration() {
    return properties -> {
      SnapshotProfilingConfiguration.SUPPLIER.configure(
          SnapshotProfilingEnvVarsConfigurationFactory.create(properties));
      return Collections.emptyMap();
    };
  }

  private BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder>
      addShutdownHook() {
    return (builder, properties) -> {
      builder.addSpanProcessor(new SdkShutdownHook());
      return builder;
    };
  }

  private BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder>
      snapshotProfilingSpanProcessor(TraceRegistry registry) {
    return (builder, properties) -> {
      double selectionProbability =
          SnapshotProfilingConfiguration.SUPPLIER.get().getSnapshotSelectionProbability();

      return builder.addSpanProcessor(
          new SnapshotProfilingSpanProcessor(
              registry, new TraceIdBasedSnapshotSelector(selectionProbability)));
    };
  }

  private Function<ConfigProperties, Map<String, String>> startTrackingActiveSpans(
      TraceRegistry registry) {
    return properties -> {
      contextStorageWrapper.wrapContextStorage(registry);
      return Collections.emptyMap();
    };
  }
}
