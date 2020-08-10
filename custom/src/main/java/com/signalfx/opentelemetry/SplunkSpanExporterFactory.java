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
