package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.testing.context.SettableContextStorageProvider;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

class ContextStorageResettingSpanTrackingActivator implements SpanTrackingActivator,
    AfterEachCallback {
  @Override
  public void activate(TraceRegistry registry) {
    ActiveSpanTracker spanTracker =
        new ActiveSpanTracker(ContextStorage.defaultStorage(), registry);
    SpanTracker.SUPPLIER.configure(spanTracker);
    SettableContextStorageProvider.setContextStorage(spanTracker);
  }

  @Override
  public void afterEach(ExtensionContext context) {
    SettableContextStorageProvider.setContextStorage(ContextStorage.defaultStorage());
  }
}
