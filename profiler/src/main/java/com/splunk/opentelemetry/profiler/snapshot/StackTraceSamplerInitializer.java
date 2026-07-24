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

import com.splunk.opentelemetry.profiler.OtelLoggerFactory;
import com.splunk.opentelemetry.profiler.util.DeclarativeConfigPropertiesUtil;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Duration;

class StackTraceSamplerInitializer {
  private StackTraceSamplerInitializer() {}

  static void setupStackTraceSampler(SnapshotProfilingConfiguration configuration) {
    Duration samplingPeriod = configuration.getSamplingInterval();
    StagingArea.SUPPLIER.configure(createStagingArea(configuration));

    StackTraceSampler sampler =
        new PeriodicStackTraceSampler(StagingArea.SUPPLIER, SpanTracker.SUPPLIER, samplingPeriod);

    StackTraceSampler.SUPPLIER.configure(sampler);
  }

  static void setupStackTraceExporter(
      SnapshotProfilingConfiguration configuration,
      Resource resource,
      OtelLoggerFactory otelLoggerFactory) {
    int maxDepth = configuration.getStackDepth();
    Logger otelLogger =
        buildLogger(otelLoggerFactory, resource, configuration.getConfigProperties());
    AsyncStackTraceExporter exporter = new AsyncStackTraceExporter(otelLogger, maxDepth);
    StackTraceExporter.SUPPLIER.configure(exporter);
  }

  private static StagingArea createStagingArea(SnapshotProfilingConfiguration configuration) {
    Duration interval = configuration.getExportInterval();
    int capacity = configuration.getStagingCapacity();
    return new PeriodicallyExportingStagingArea(StackTraceExporter.SUPPLIER, interval, capacity);
  }

  private static Logger buildLogger(
      OtelLoggerFactory otelLoggerFactory, Resource resource, Object configProperties) {
    if (configProperties instanceof DeclarativeConfigProperties) {
      DeclarativeConfigProperties exporterConfig =
          DeclarativeConfigPropertiesUtil.getStructuredOrEmpty(
              (DeclarativeConfigProperties) configProperties, "exporter");
      return otelLoggerFactory.build(exporterConfig, resource);
    }
    if (configProperties instanceof ConfigProperties) {
      return otelLoggerFactory.build((ConfigProperties) configProperties, resource);
    }
    throw new IllegalArgumentException(
        "Unsupported config properties type: " + configProperties.getClass().getName());
  }
}
