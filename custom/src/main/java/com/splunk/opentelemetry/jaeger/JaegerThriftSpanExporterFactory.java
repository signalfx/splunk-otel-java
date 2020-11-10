package com.splunk.opentelemetry.jaeger;

import io.opentelemetry.exporter.jaeger.thrift.JaegerThriftSpanExporter;
import io.opentelemetry.javaagent.spi.exporter.SpanExporterFactory;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

//TODO remove when https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/1609 is implemented
public class JaegerThriftSpanExporterFactory implements SpanExporterFactory {
  @Override
  public SpanExporter fromConfig(Properties config) {
    return JaegerThriftSpanExporter.builder().readProperties(config).build();
  }

  @Override
  public Set<String> getNames() {
    return Collections.singleton("jaeger-thrift");
  }
}
