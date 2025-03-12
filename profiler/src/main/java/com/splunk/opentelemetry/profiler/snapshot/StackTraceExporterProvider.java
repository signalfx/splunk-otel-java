package com.splunk.opentelemetry.profiler.snapshot;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

class StackTraceExporterProvider implements Supplier<StackTraceExporter> {
  private StackTraceExporter exporter;

  @Override
  public StackTraceExporter get() {
    if (exporter == null) {
      return StackTraceExporter.NOOP;
    }
    return exporter;
  }

  void configure(StackTraceExporter exporter) {
    AtomicReference<StackTraceExporter> exporterRef = new AtomicReference<>();
    exporterRef.set(exporter);
    this.exporter = exporter;
  }
}
