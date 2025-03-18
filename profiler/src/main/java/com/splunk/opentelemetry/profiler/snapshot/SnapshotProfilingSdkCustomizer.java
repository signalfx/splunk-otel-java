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
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class SnapshotProfilingSdkCustomizer implements AutoConfigurationCustomizerProvider {
  private final TraceRegistry registry;
  private final StackTraceSampler sampler;

  public SnapshotProfilingSdkCustomizer() {
    this(new TraceRegistry(), new ScheduledExecutorStackTraceSampler(new NoopStagingArea(), ActiveSpanTracker.INSTANCE));
  }

  @VisibleForTesting
  SnapshotProfilingSdkCustomizer(TraceRegistry registry, StackTraceSampler sampler) {
    this.registry = registry;
    this.sampler = sampler;
  }

  @Override
  public void customize(AutoConfigurationCustomizer autoConfigurationCustomizer) {
    autoConfigurationCustomizer
        .addPropertiesCustomizer(autoConfigureSnapshotVolumePropagator())
        .addTracerProviderCustomizer(snapshotProfilingSpanProcessor(registry))
        .addPropertiesCustomizer(startTrackingActiveSpans(registry));
  }

  private BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder>
      snapshotProfilingSpanProcessor(TraceRegistry registry) {
    return (builder, properties) -> {
      if (snapshotProfilingEnabled(properties)) {
        return builder.addSpanProcessor(new SnapshotProfilingSpanProcessor(registry, sampler));
      }
      return builder;
    };
  }

  /**
   * Attempt to autoconfigure the OpenTelemetry propagators to include the Splunk snapshot volume
   * propagator and ensure it runs after the W3C Baggage propagator and ensure that a trace context
   * propagator is configured. In addition, take care to retain any propagators explicitly
   * configured prior.
   *
   * <p>The Java agent uses the "otel.propagators" property and the value is assumed to be a comma
   * seperated list of propagator names. See <a
   * href="https://opentelemetry.io/docs/languages/java/configuration/#properties-general">OpenTelemetry's
   * Java Agent Configuration</a> for more details.
   */
  private Function<ConfigProperties, Map<String, String>> autoConfigureSnapshotVolumePropagator() {
    return properties -> {
      if (snapshotProfilingEnabled(properties)) {
        Set<String> propagators = new LinkedHashSet<>(properties.getList("otel.propagators"));
        if (propagators.contains("none")) {
          return Collections.emptyMap();
        }

        if (includeTraceContextPropagator(propagators)) {
          propagators.add("tracecontext");
        }
        propagators.add("baggage");
        propagators.add(SnapshotVolumePropagatorProvider.NAME);
        return Collections.singletonMap("otel.propagators", String.join(",", propagators));
      }
      return Collections.emptyMap();
    };
  }

  private boolean includeTraceContextPropagator(Set<String> configuredPropagators) {
    return configuredPropagators.isEmpty();
  }

  private static boolean INTERCEPT_OTEL_CONTEXT = true;

  private Function<ConfigProperties, Map<String, String>> startTrackingActiveSpans(TraceRegistry registry) {
    return properties -> {
      if (snapshotProfilingEnabled(properties)) {
        if (INTERCEPT_OTEL_CONTEXT) {
          ContextStorage.addWrapper(contextStorage -> {
            ActiveSpanTracker.configure(contextStorage);
            return ActiveSpanTracker.INSTANCE;
          });
          ActiveSpanTracker.INSTANCE.configure(registry);
          INTERCEPT_OTEL_CONTEXT = false;
        }
      }
      return Collections.emptyMap();
    };
  }

  private boolean snapshotProfilingEnabled(ConfigProperties config) {
    return config.getBoolean(CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER, false);
  }
}
