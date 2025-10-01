package com.splunk.opentelemetry.profiler.snapshot.registry;

public class TraceRegistryHolder {
  private final static TraceRegistry registry = new TraceRegistry();
  private TraceRegistryHolder() {}

  public static TraceRegistry getTraceRegistry() {
    return registry;
  }
}
