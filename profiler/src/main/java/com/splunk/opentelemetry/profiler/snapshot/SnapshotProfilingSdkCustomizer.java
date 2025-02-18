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

import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER;

import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import java.util.function.BiFunction;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class SnapshotProfilingSdkCustomizer implements AutoConfigurationCustomizerProvider {
  private final TraceRegistry registry;

  public SnapshotProfilingSdkCustomizer() {
    this(new TraceRegistry());
  }

  @VisibleForTesting
  SnapshotProfilingSdkCustomizer(TraceRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void customize(AutoConfigurationCustomizer autoConfigurationCustomizer) {
    autoConfigurationCustomizer
        .addTracerProviderCustomizer(snapshotProfilingSpanProcessor(registry));
//        .addPropagatorCustomizer(addProfilingSignalPropagator(registry));
  }

  private BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder>
      snapshotProfilingSpanProcessor(TraceRegistry registry) {
    return (builder, properties) -> {
      if (snapshotProfilingEnabled(properties)) {
        return builder.addSpanProcessor(new SnapshotProfilingSpanProcessor(registry));
      }
      return builder;
    };
  }

  private BiFunction<TextMapPropagator, ConfigProperties, TextMapPropagator>
      addProfilingSignalPropagator(TraceRegistry registry) {
    return (textMapPropagator, properties) -> {
      if (snapshotProfilingEnabled(properties)
          && textMapPropagator instanceof W3CTraceContextPropagator) {
        return TextMapPropagator.composite(
            textMapPropagator, new SnapshotProfilingSignalPropagator(registry));
      }
      return textMapPropagator;
    };
  }

  private boolean snapshotProfilingEnabled(ConfigProperties config) {
    return config.getBoolean(CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER, false);
  }
}
