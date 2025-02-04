package com.splunk.opentelemetry.profiler.snapshot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import java.util.Random;
import org.junit.jupiter.api.Test;

class TraceRegistryTest {
  private static final Random RANDOM = new Random();

  private final TraceRegistry registry = new TraceRegistry();

  @Test
  void registerTrace() {
    var spanContext = newSpanContext();
    registry.register(spanContext);
    assertTrue(registry.isRegistered(spanContext));
  }

  @Test
  void unregisteredTracesAreNotRegisteredForProfiling() {
    var spanContext = newSpanContext();
    assertFalse(registry.isRegistered(spanContext));
  }

  @Test
  void unregisterTraceForProfiling() {
    var spanContext = newSpanContext();

    registry.register(spanContext);
    registry.unregister(spanContext);

    assertFalse(registry.isRegistered(spanContext));
  }

  private SpanContext newSpanContext() {
    return newSpanContext(randomTraceId());
  }

  private SpanContext newSpanContext(String traceId) {
    var spanId = SpanId.fromLong(RANDOM.nextLong());
    return SpanContext.create(traceId, spanId, TraceFlags.getDefault(), TraceState.getDefault());
  }

  private String randomTraceId() {
    return TraceId.fromLongs(RANDOM.nextLong(), RANDOM.nextLong());
  }
}
