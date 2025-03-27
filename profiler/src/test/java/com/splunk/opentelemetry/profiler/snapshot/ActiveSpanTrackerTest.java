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

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.IdGenerator;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ActiveSpanTrackerTest {
  private final ContextStorage storage = ContextStorage.get();
  private final TogglableTraceRegistry registry = new TogglableTraceRegistry();
  private final ActiveSpanTracker spanTracker = new ActiveSpanTracker(storage, registry);

  @Test
  void currentContextComesFromOpenTelemetryContextStorage() {
    var context = Context.root().with(ContextKey.named("test-key"), "value");
    try (var ignored = spanTracker.attach(context)) {
      assertEquals(storage.current(), spanTracker.current());
    }
  }

  @Test
  void noActiveSpanBeforeFirstSpanInTraceIsAttachedToContext() {
    var traceId = IdGenerator.random().generateTraceId();
    assertEquals(Optional.empty(), spanTracker.getActiveSpan(traceId));
  }

  @Test
  void trackActiveSpanWhenNewContextAttached() {
    var span = FakeSpan.newSpan(Snapshotting.spanContext());
    var spanContext = span.getSpanContext();
    var context = Context.root().with(span);
    registry.register(spanContext);

    try (var ignored = spanTracker.attach(context)) {
      var traceId = span.getSpanContext().getTraceId();
      assertEquals(Optional.of(spanContext), spanTracker.getActiveSpan(traceId));
    }
  }

  @Test
  void noActiveSpanForTraceAfterSpansScopeIsClosed() {
    var span = FakeSpan.newSpan(Snapshotting.spanContext());
    var spanContext = span.getSpanContext();
    var context = Context.root().with(span);
    registry.register(spanContext);

    var scope = spanTracker.attach(context);
    scope.close();

    assertEquals(Optional.empty(), spanTracker.getActiveSpan(spanContext.getTraceId()));
  }

  @Test
  void trackActiveSpanAcrossMultipleContextChanges() {
    var root = FakeSpan.newSpan(Snapshotting.spanContext());
    var context = Context.root().with(root);
    registry.register(root.getSpanContext());

    try (var ignoredScope1 = spanTracker.attach(context)) {
      var span = FakeSpan.newSpan(Snapshotting.spanContext().withTraceIdFrom(root));
      var spanContext = span.getSpanContext();
      context = context.with(span);
      try (var ignoredScope2 = spanTracker.attach(context)) {
        var traceId = span.getSpanContext().getTraceId();
        assertEquals(Optional.of(spanContext), spanTracker.getActiveSpan(traceId));
      }
    }
  }

  @Test
  void restoreActiveSpanToPreviousSpanAfterScopeClosing() {
    var root = FakeSpan.newSpan(Snapshotting.spanContext());
    var context = Context.root().with(root);
    registry.register(root.getSpanContext());

    try (var ignoredScope1 = spanTracker.attach(context)) {
      var span = FakeSpan.newSpan(Snapshotting.spanContext().withTraceIdFrom(root));
      context = context.with(span);

      var scope = spanTracker.attach(context);
      scope.close();

      var rootSpanContext = root.getSpanContext();
      assertEquals(
          Optional.of(rootSpanContext), spanTracker.getActiveSpan(rootSpanContext.getTraceId()));
    }
  }

  @Test
  void trackActiveSpanForMultipleTraces() throws Exception {
    var executor = Executors.newSingleThreadExecutor();
    var root1 = FakeSpan.newSpan(Snapshotting.spanContext());
    var root2 = FakeSpan.newSpan(Snapshotting.spanContext());
    registry.register(root1.getSpanContext());
    registry.register(root2.getSpanContext());

    try (var ignoredScope1 = executor.submit(attach(root1)).get();
        var ignoredScope2 = executor.submit(attach(root2)).get()) {
      var traceId1 = root1.getSpanContext().getTraceId();
      assertEquals(Optional.of(root1.getSpanContext()), spanTracker.getActiveSpan(traceId1));
      var traceId2 = root2.getSpanContext().getTraceId();
      assertEquals(Optional.of(root2.getSpanContext()), spanTracker.getActiveSpan(traceId2));
    } finally {
      executor.shutdown();
    }
  }

  private Callable<Scope> attach(Span span) {
    return () -> {
      var context = Context.root().with(span);
      return spanTracker.attach(context);
    };
  }

  @Test
  void doNotTrackSpanWhenNoSpanPresentInContext() {
    var context = Context.root().with(ContextKey.named("test-key"), "value");
    try (var ignored = spanTracker.attach(context)) {
      assertEquals(Optional.empty(), spanTracker.getActiveSpan(TraceId.getInvalid()));
    }
  }

  @Test
  void doNotTrackSpanWhenSpanIsNotSampled() {
    var span = FakeSpan.newSpan(Snapshotting.spanContext().unsampled());
    var context = Context.root().with(span);
    registry.register(span.getSpanContext());

    try (var ignored = spanTracker.attach(context)) {
      var traceId = span.getSpanContext().getTraceId();
      assertEquals(Optional.empty(), spanTracker.getActiveSpan(traceId));
    }
  }

  @Test
  void doNotTrackSpanTraceIsNotRegisteredForSnapshotting() {
    registry.toggle(TogglableTraceRegistry.State.OFF);

    var span = FakeSpan.newSpan(Snapshotting.spanContext());
    var context = Context.root().with(span);

    try (var ignored = spanTracker.attach(context)) {
      var traceId = span.getSpanContext().getTraceId();
      assertEquals(Optional.empty(), spanTracker.getActiveSpan(traceId));
    }
  }

  @Test
  void doNotTrackContinuallyTrackSameSpan() {
    var span = FakeSpan.newSpan(Snapshotting.spanContext());
    var context = Context.root().with(span);
    registry.register(span.getSpanContext());

    try (var ignored = spanTracker.attach(context)) {
      var newContext = context.with(ContextKey.named("test-key"), "value");
      try (var scope = spanTracker.attach(newContext)) {
        assertEquals(
            "io.opentelemetry.context.ThreadLocalContextStorage$ScopeImpl",
            scope.getClass().getName());
      }
    }
  }

  private static class FakeSpan implements Span {
    static FakeSpan newSpan(Snapshotting.SpanContextBuilder spanContext) {
      return new FakeSpan(spanContext.build());
    }

    private final SpanContext spanContext;

    private FakeSpan(SpanContext spanContext) {
      this.spanContext = spanContext;
    }

    @Override
    public <T> Span setAttribute(AttributeKey<T> key, T value) {
      return this;
    }

    @Override
    public Span addEvent(String name, Attributes attributes) {
      return this;
    }

    @Override
    public Span addEvent(String name, Attributes attributes, long timestamp, TimeUnit unit) {
      return this;
    }

    @Override
    public Span setStatus(StatusCode statusCode, String description) {
      return this;
    }

    @Override
    public Span recordException(Throwable exception, Attributes additionalAttributes) {
      return this;
    }

    @Override
    public Span updateName(String name) {
      return this;
    }

    @Override
    public void end() {}

    @Override
    public void end(long timestamp, TimeUnit unit) {}

    @Override
    public SpanContext getSpanContext() {
      return spanContext;
    }

    @Override
    public boolean isRecording() {
      return true;
    }
  }
}
