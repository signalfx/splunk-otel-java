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

package com.splunk.opentelemetry.profiler;

import com.splunk.opentelemetry.logs.InstrumentationLibraryLogsAdapter;
import com.splunk.opentelemetry.logs.LogDataAdapter;
import com.splunk.opentelemetry.logs.LogsExporter;
import com.splunk.opentelemetry.logs.OtlpLogsExporter;
import com.splunk.opentelemetry.logs.OtlpLogsExporterBuilder;
import com.splunk.opentelemetry.logs.ResourceLogsAdapter;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.tooling.config.ConfigPropertiesAdapter;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetryResourceAutoConfiguration;
import io.opentelemetry.sdk.resources.Resource;

class LogsExporterBuilder {

  private static final String OTEL_INSTRUMENTATION_NAME = "otel.profiling";
  private static final String OTEL_INSTRUMENTATION_VERSION = "0.1.0";

  static LogsExporter fromConfig(Config config) {
    ResourceLogsAdapter adapter = buildResourceLogsAdapter(config);

    OtlpLogsExporterBuilder builder =
        OtlpLogsExporter.builder()
            .setAdapter(adapter)
            .addHeader("Extra-Content-Type", "otel-profiling-stacktraces");

    String ingestUrl = Configuration.getConfigUrl(config);
    if (ingestUrl != null) {
      builder.setEndpoint(ingestUrl);
    }
    return builder.build();
  }

  private static ResourceLogsAdapter buildResourceLogsAdapter(Config config) {
    Resource resource =
        OpenTelemetryResourceAutoConfiguration.configureResource(
            new ConfigPropertiesAdapter(config));
    LogDataAdapter logDataAdapter = new LogDataAdapter();
    InstrumentationLibraryLogsAdapter instLibraryLogsAdapter =
        InstrumentationLibraryLogsAdapter.builder()
            .logEntryAdapter(logDataAdapter)
            .instrumentationName(OTEL_INSTRUMENTATION_NAME)
            .instrumentationVersion(OTEL_INSTRUMENTATION_VERSION)
            .build();
    return new ResourceLogsAdapter(instLibraryLogsAdapter, resource);
  }
}
