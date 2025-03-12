package com.splunk.opentelemetry.profiler.snapshot;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class StackTraceExporterProviderTest {
  private final StackTraceExporterProvider provider = new StackTraceExporterProvider();

  @Test
  void provideNoopExporterWhenNotConfigured() {
    assertSame(StackTraceExporter.NOOP, provider.get());
  }

  @Test
  void providedConfiguredExporter() {
    var exporter = new InMemoryStackTraceExporter();
    provider.configure(exporter);
    assertSame(exporter, provider.get());
  }
}
