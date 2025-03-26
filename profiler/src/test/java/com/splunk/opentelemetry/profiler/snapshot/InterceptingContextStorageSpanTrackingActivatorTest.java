package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.context.ContextStorage;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class InterceptingContextStorageSpanTrackingActivatorTest {
  private final ContextStorageUnaryOperator delegate = new ContextStorageUnaryOperator();
  private final InterceptingContextStorageSpanTrackingActivator activator = new InterceptingContextStorageSpanTrackingActivator(delegate);

  @Test
  void interceptContextStorage() {
    activator.activate(new TraceRegistry());
    assertInstanceOf(ActiveSpanTracker.class, delegate.storage);
  }

  @Test
  void activateSpanTracker() {
    activator.activate(new TraceRegistry());
    assertInstanceOf(ActiveSpanTracker.class, SpanTrackerProvider.INSTANCE.get());
  }

  private static class ContextStorageUnaryOperator implements Consumer<UnaryOperator<ContextStorage>> {
    private ContextStorage storage = ContextStorage.defaultStorage();

    @Override
    public void accept(UnaryOperator<ContextStorage> operator) {
      storage = operator.apply(storage);
    }
  }
}
