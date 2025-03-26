package com.splunk.opentelemetry.profiler.snapshot;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.context.ContextStorage;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

class InterceptingContextStorageSpanTrackingActivator implements SpanTrackingActivator {
  private final Consumer<UnaryOperator<ContextStorage>> contextStorageWrappingFunction;

  InterceptingContextStorageSpanTrackingActivator() {
    this(ContextStorage::addWrapper);
  }

  @VisibleForTesting
  InterceptingContextStorageSpanTrackingActivator(Consumer<UnaryOperator<ContextStorage>> contextStorageWrappingFunction) {
    this.contextStorageWrappingFunction = contextStorageWrappingFunction;
  }

  @Override
  public void activate(TraceRegistry registry) {
    contextStorageWrappingFunction.accept(contextStorage -> {
      ActiveSpanTracker tracker = new ActiveSpanTracker(contextStorage, registry);
      SpanTrackerProvider.INSTANCE.configure(tracker);
      return tracker;
    });
  }
}
