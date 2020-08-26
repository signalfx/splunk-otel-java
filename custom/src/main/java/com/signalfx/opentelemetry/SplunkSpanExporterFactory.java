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

package com.signalfx.opentelemetry;

import io.opentelemetry.auto.exporters.jaeger.JaegerExporterFactory;
import io.opentelemetry.auto.exporters.logging.LoggingExporterFactory;
import io.opentelemetry.auto.exporters.otlp.OtlpSpanExporterFactory;
import io.opentelemetry.auto.exporters.zipkin.ZipkinExporterFactory;
import io.opentelemetry.sdk.extensions.auto.config.Config;
import io.opentelemetry.sdk.extensions.auto.config.SpanExporterFactory;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class SplunkSpanExporterFactory implements SpanExporterFactory {
  @Override
  public SpanExporter fromConfig(Config config) {
    String delegateExporter = config.getString("splunk.delegate", "otlp");
    SpanExporter delegate;
    switch (delegateExporter) {
      case "logging":
        delegate = new LoggingExporterFactory().fromConfig(config);
        break;
      case "zipkin":
        delegate = new ZipkinExporterFactory().fromConfig(config);
        break;
      case "jaeger":
        delegate = new JaegerExporterFactory().fromConfig(config);
        break;
      case "otlp":
      default:
        delegate = new OtlpSpanExporterFactory().fromConfig(config);
    }
    return new SplunkSpanExporter(delegate);
  }
}
