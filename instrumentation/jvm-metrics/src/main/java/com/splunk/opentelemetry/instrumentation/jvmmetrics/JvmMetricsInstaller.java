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

package com.splunk.opentelemetry.instrumentation.jvmmetrics;

import static java.util.Collections.singleton;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

@AutoService(AgentListener.class)
public class JvmMetricsInstaller implements AgentListener {
  @Override
  public void afterAgent(
      Config config, AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    boolean metricsRegistryPresent = !Metrics.globalRegistry.getRegistries().isEmpty();
    if (!config.isInstrumentationEnabled(singleton("jvm-metrics"), metricsRegistryPresent)) {
      return;
    }

    new ClassLoaderMetrics().bindTo(Metrics.globalRegistry);
    new JvmGcMetrics().bindTo(Metrics.globalRegistry);
    new JvmMemoryMetrics().bindTo(Metrics.globalRegistry);
    new JvmThreadMetrics().bindTo(Metrics.globalRegistry);
  }
}
