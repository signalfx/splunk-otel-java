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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.trace.IdGenerator;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;

class Snapshotting {
  private static final Random RANDOM = new Random();

  static SnapshotProfilingSdkCustomizerBuilder customizer() {
    return new SnapshotProfilingSdkCustomizerBuilder();
  }

  static StackTraceBuilder stackTrace() {
    var threadId = RANDOM.nextLong(10_000);
    return new StackTraceBuilder()
        .with(Instant.now())
        .with(Duration.ofMillis(20))
        .withTraceId(IdGenerator.random().generateTraceId())
        .withSpanId(IdGenerator.random().generateSpanId())
        .withId(threadId)
        .withName("thread-" + threadId)
        .with(Thread.State.WAITING)
        .with(new RuntimeException());
  }

  static SpanContextBuilder spanContext() {
    return new SpanContextBuilder();
  }

  static class SpanContextBuilder {
    private SpanContext spanContext = SpanContext.create(IdGenerator.random().generateTraceId(),
        IdGenerator.random().generateSpanId(), TraceFlags.getSampled(), TraceState.getDefault());

    SpanContextBuilder withTraceIdFrom(Span span) {
      return withTraceId(span.getSpanContext().getTraceId());
    }

    SpanContextBuilder withTraceId(String traceId) {
      spanContext = SpanContext.create(traceId, spanContext.getSpanId(), spanContext.getTraceFlags(), spanContext.getTraceState());
      return this;
    }

    SpanContextBuilder withSpanId(String spanId) {
      spanContext = SpanContext.create(spanContext.getTraceId(), spanId, spanContext.getTraceFlags(), spanContext.getTraceState());
      return this;
    }

    SpanContextBuilder unsampled() {
      spanContext = SpanContext.create(spanContext.getTraceId(), spanContext.getSpanId(), TraceFlags.getDefault(), spanContext.getTraceState());
      return this;
    }

    SpanContext build() {
      return spanContext;
    }
  }

  private Snapshotting() {}
}
