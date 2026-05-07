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

import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getConfig;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.instrumentation.jvmmetrics.otel.OtelAllocatedMemoryMetrics;
import com.splunk.opentelemetry.instrumentation.jvmmetrics.otel.OtelGcMemoryMetrics;
import com.splunk.opentelemetry.profiler.ProfilerConfiguration;
import com.splunk.opentelemetry.profiler.ProfilerDeclarativeConfiguration;
import com.splunk.opentelemetry.profiler.ProfilerEnvVarsConfiguration;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

@AutoService(AgentListener.class)
public class JvmMetricsInstaller implements AgentListener {
  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    ConfigProperties config = getConfig(autoConfiguredOpenTelemetrySdk);
    boolean metricsEnabled = isProfilerMemoryEnabled(config);

    if (!config.getBoolean("otel.instrumentation.jvm-metrics-splunk.enabled", metricsEnabled)) {
      return;
    }

    new OtelAllocatedMemoryMetrics().install();
    new OtelGcMemoryMetrics().install();
  }

  private boolean isProfilerMemoryEnabled(ConfigProperties config) {
    ProfilerConfiguration profilerConfiguration =
        ProfilerDeclarativeConfiguration.SUPPLIER.isConfigured()
            ? ProfilerDeclarativeConfiguration.SUPPLIER.get()
            : new ProfilerEnvVarsConfiguration(config);

    return profilerConfiguration.getMemoryEnabled();
  }
}
