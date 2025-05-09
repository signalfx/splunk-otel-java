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
import com.splunk.opentelemetry.profiler.OtelLoggerFactory;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;

@AutoService(AgentListener.class)
public class StackTraceExporterActivator implements AgentListener {
  private static final java.util.logging.Logger logger =
      java.util.logging.Logger.getLogger(StackTraceExporterActivator.class.getName());

  private final OtelLoggerFactory otelLoggerFactory;

  public StackTraceExporterActivator() {
    this(new OtelLoggerFactory());
  }

  @VisibleForTesting
  StackTraceExporterActivator(OtelLoggerFactory otelLoggerFactory) {
    this.otelLoggerFactory = otelLoggerFactory;
  }

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk sdk) {
    ConfigProperties properties = AutoConfigureUtil.getConfig(sdk);
    if (!snapshotProfilingEnabled(properties)) {
      return;
    }

    SnapshotProfilingConfiguration.log(properties);

    int maxDepth = SnapshotProfilingConfiguration.getSnapshotProfilerStackDepth(properties);
    Logger otelLogger = buildLogger(sdk, properties);
    AsyncStackTraceExporter exporter = new AsyncStackTraceExporter(otelLogger, maxDepth);
    StackTraceExporter.SUPPLIER.configure(exporter);

    logger.info("Snapshot profiling is active.");
  }

  private boolean snapshotProfilingEnabled(ConfigProperties properties) {
    return SnapshotProfilingConfiguration.isSnapshotProfilingEnabled(properties);
  }

  private Logger buildLogger(AutoConfiguredOpenTelemetrySdk sdk, ConfigProperties properties) {
    Resource resource = AutoConfigureUtil.getResource(sdk);
    return otelLoggerFactory.build(properties, resource);
  }
}
