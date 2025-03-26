package com.splunk.opentelemetry.profiler.snapshot;

interface SpanTrackingActivator {
  void activate(TraceRegistry registry);
}
