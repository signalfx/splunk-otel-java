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

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import java.util.function.Function;

public class OtelLoggerFactory {
  private final Function<ConfigProperties, LogRecordExporter> logRecordExporter;

  public OtelLoggerFactory() {
    this(LogExporterBuilder::fromConfig);
  }

  @VisibleForTesting
  public OtelLoggerFactory(Function<ConfigProperties, LogRecordExporter> logRecordExporter) {
    this.logRecordExporter = logRecordExporter;
  }

  public Logger build(ConfigProperties properties, Resource resource) {
    LogRecordExporter exporter = createLogRecordExporter(properties);
    LogRecordProcessor processor = SimpleLogRecordProcessor.create(exporter);
    return buildOtelLogger(processor, resource);
  }

  private LogRecordExporter createLogRecordExporter(ConfigProperties properties) {
    return logRecordExporter.apply(properties);
  }

  private Logger buildOtelLogger(LogRecordProcessor logProcessor, Resource resource) {
    return SdkLoggerProvider.builder()
        .addLogRecordProcessor(logProcessor)
        .setResource(resource)
        .build()
        .loggerBuilder(ProfilingSemanticAttributes.OTEL_INSTRUMENTATION_NAME)
        .setInstrumentationVersion(ProfilingSemanticAttributes.OTEL_INSTRUMENTATION_VERSION)
        .build();
  }
}
