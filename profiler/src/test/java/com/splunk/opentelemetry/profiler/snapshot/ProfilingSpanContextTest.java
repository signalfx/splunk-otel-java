/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
