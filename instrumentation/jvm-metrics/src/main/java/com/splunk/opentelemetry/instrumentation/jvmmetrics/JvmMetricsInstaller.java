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

import static com.splunk.opentelemetry.SplunkConfiguration.METRICS_ENABLED_PROPERTY;
import static com.splunk.opentelemetry.SplunkConfiguration.METRICS_IMPLEMENTATION;
import static com.splunk.opentelemetry.SplunkConfiguration.PROFILER_MEMORY_ENABLED_PROPERTY;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.instrumentation.jvmmetrics.micrometer.MicrometerAllocatedMemoryMetrics;
import com.splunk.opentelemetry.instrumentation.jvmmetrics.micrometer.MicrometerGcMemoryMetrics;
import com.splunk.opentelemetry.instrumentation.jvmmetrics.otel.OtelAllocatedMemoryMetrics;
import com.splunk.opentelemetry.instrumentation.jvmmetrics.otel.OtelGcMemoryMetrics;
import com.splunk.opentelemetry.instrumentation.jvmmetrics.otel.OtelJvmGcMetrics;
import com.splunk.opentelemetry.instrumentation.jvmmetrics.otel.OtelJvmHeapPressureMetrics;
import com.splunk.opentelemetry.instrumentation.jvmmetrics.otel.OtelJvmMemoryMetrics;
import com.splunk.opentelemetry.instrumentation.jvmmetrics.otel.OtelJvmThreadMetrics;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

@AutoService(AgentListener.class)
public class JvmMetricsInstaller implements AgentListener {

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    ConfigProperties config = autoConfiguredOpenTelemetrySdk.getConfig();
    boolean metricsEnabled = config.getBoolean(METRICS_ENABLED_PROPERTY, false);
    if (!config.getBoolean("otel.instrumentation.jvm-metrics.splunk.enabled", metricsEnabled)) {
      return;
    }

    String metricsImplementation = config.getString(METRICS_IMPLEMENTATION);

    if (useMicrometerMetrics(metricsImplementation)) {
      new ClassLoaderMetrics().bindTo(Metrics.globalRegistry);
      new JvmGcMetrics().bindTo(Metrics.globalRegistry);
      new JvmHeapPressureMetrics().bindTo(Metrics.globalRegistry);
      new JvmMemoryMetrics().bindTo(Metrics.globalRegistry);
      new JvmThreadMetrics().bindTo(Metrics.globalRegistry);
    }
    if (useOtelMetrics(metricsImplementation)) {
      new OtelJvmGcMetrics().install();
      new OtelJvmHeapPressureMetrics().install();
      new OtelJvmMemoryMetrics().install();
      new OtelJvmThreadMetrics().install();
    }

    // Following metrics are experimental, we'll enable them only when memory profiling is enabled
    if (config.getBoolean(PROFILER_MEMORY_ENABLED_PROPERTY, false)
        || config.getBoolean("splunk.metrics.experimental.enabled", false)) {
      if (useMicrometerMetrics(metricsImplementation)) {
        new MicrometerAllocatedMemoryMetrics().bindTo(Metrics.globalRegistry);
        new MicrometerGcMemoryMetrics().bindTo(Metrics.globalRegistry);
      }
      if (useOtelMetrics(metricsImplementation)) {
        new OtelAllocatedMemoryMetrics().install();
        new OtelGcMemoryMetrics().install();
      }
    }
  }

  private static boolean useMicrometerMetrics(String metricsImplementation) {
    return "micrometer".equalsIgnoreCase(metricsImplementation);
  }

  private static boolean useOtelMetrics(String metricsImplementation) {
    return "opentelemetry".equalsIgnoreCase(metricsImplementation);
  }
}
