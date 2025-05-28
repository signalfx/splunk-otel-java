package com.splunk.opentelemetry.profiler.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceId;
import org.junit.jupiter.api.Test;

class ProfilingSpanContextTest {
  @Test
  void createFromOpenTelemetrySpanContext() {
    var spanContext = Snapshotting.spanContext().build();
    var profilingSpanContext = ProfilingSpanContext.from(spanContext);

    assertEquals(spanContext.getTraceId(), profilingSpanContext.getTraceId());
    assertEquals(spanContext.getSpanId(), profilingSpanContext.getSpanId());
  }

  @Test
  void equals() {
    var spanContext = Snapshotting.spanContext().build();

    var one = ProfilingSpanContext.from(spanContext);
    var two = ProfilingSpanContext.from(spanContext);

    assertThat(one.equals(two)).isTrue();
    assertThat(two.equals(one)).isTrue();
  }

  @Test
  void notEquals() {
    var one = ProfilingSpanContext.from(Snapshotting.spanContext().build());
    var two = ProfilingSpanContext.from(Snapshotting.spanContext().build());

    assertThat(one.equals(two)).isFalse();
    assertThat(one.equals(new Object())).isFalse();
    assertThat(two.equals(one)).isFalse();
    assertThat(two.equals(new Object())).isFalse();
  }

  @Test
  void hashCodeEquals() {
    var spanContext = Snapshotting.spanContext().build();

    var one = ProfilingSpanContext.from(spanContext);
    var two = ProfilingSpanContext.from(spanContext);

    assertEquals(one.hashCode(), two.hashCode());
  }

  @Test
  void hasCodeNotEquals() {
    var one = ProfilingSpanContext.from(Snapshotting.spanContext().build());
    var two = ProfilingSpanContext.from(Snapshotting.spanContext().build());

    assertNotEquals(one.hashCode(), two.hashCode());
  }

  @Test
  void invalidProfilingSpanContext() {
    var invalid = ProfilingSpanContext.INVALID;
    assertEquals(TraceId.getInvalid(), invalid.getTraceId());
    assertEquals(SpanId.getInvalid(), invalid.getSpanId());
  }
}
