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
import com.splunk.opentelemetry.profiler.OtelLoggerFactory;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;

@AutoService(AgentListener.class)
public class StackTraceExporterActivator implements AgentListener {
  private final OtelLoggerFactory otelLoggerFactory = new OtelLoggerFactory();

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    ConfigProperties properties = AutoConfigureUtil.getConfig(autoConfiguredOpenTelemetrySdk);
    if (snapshotProfilingEnabled(properties)) {
      Resource resource = AutoConfigureUtil.getResource(autoConfiguredOpenTelemetrySdk);
      Logger logger = otelLoggerFactory.build(properties, resource);
      AsyncStackTraceExporter exporter = new AsyncStackTraceExporter(logger);
      StackTraceExporterProvider.INSTANCE.configure(exporter);
    }
  }

  private boolean snapshotProfilingEnabled(ConfigProperties config) {
    return config.getBoolean(CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER, false);
  }
}
