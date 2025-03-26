package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.context.ContextStorage;

class InterceptingContextStorageSpanTrackingActivator implements SpanTrackingActivator {
  private static boolean INTERCEPT_OTEL_CONTEXT = true;

  @Override
  public void activate(TraceRegistry registry) {
    if (INTERCEPT_OTEL_CONTEXT) {
      ContextStorage.addWrapper(contextStorage -> {
        ActiveSpanTracker.configure(contextStorage);
        return ActiveSpanTracker.INSTANCE;
      });
      ActiveSpanTracker.INSTANCE.configure(registry);
      INTERCEPT_OTEL_CONTEXT = false;
    }
  }
}
