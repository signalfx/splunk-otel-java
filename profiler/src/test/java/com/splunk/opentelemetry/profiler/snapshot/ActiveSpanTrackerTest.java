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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.IdGenerator;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
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
  void noActiveSpanForThreadBeforeFirstSpanInTraceIsAttachedToContext() {
    assertEquals(Optional.empty(), spanTracker.getActiveSpan(currentThreadId()));
  }

  @Test
  void trackActiveSpanWhenNewContextAttached() {
    var span = Span.wrap(Snapshotting.spanContext().build());
    var spanContext = span.getSpanContext();
    var context = Context.root().with(span);
    registry.register(spanContext);

    try (var ignored = spanTracker.attach(context)) {
      assertEquals(Optional.of(spanContext), spanTracker.getActiveSpan(currentThreadId()));
    }
  }

  @Test
  void noActiveSpanForTraceAfterSpansScopeIsClosed() {
    var span = Span.wrap(Snapshotting.spanContext().build());
    var spanContext = span.getSpanContext();
    var context = Context.root().with(span);
    registry.register(spanContext);

    var scope = spanTracker.attach(context);
    scope.close();

    assertEquals(Optional.empty(), spanTracker.getActiveSpan(currentThreadId()));
  }

  @Test
  void trackActiveSpanAcrossMultipleContextChanges() {
    var root = Span.wrap(Snapshotting.spanContext().build());
    var context = Context.root().with(root);
    registry.register(root.getSpanContext());

    try (var ignoredScope1 = spanTracker.attach(context)) {
      var span = Span.wrap(Snapshotting.spanContext().withTraceIdFrom(root).build());
      var spanContext = span.getSpanContext();
      context = context.with(span);
      try (var ignoredScope2 = spanTracker.attach(context)) {
        assertEquals(Optional.of(spanContext), spanTracker.getActiveSpan(currentThreadId()));
      }
    }
  }

  @Test
  void restoreActiveSpanToPreviousSpanAfterScopeClosing() {
    var root = Span.wrap(Snapshotting.spanContext().build());
    var context = Context.root().with(root);
    registry.register(root.getSpanContext());

    try (var ignoredScope1 = spanTracker.attach(context)) {
      var span = Span.wrap(Snapshotting.spanContext().withTraceIdFrom(root).build());
      context = context.with(span);

      var scope = spanTracker.attach(context);
      scope.close();

      var rootSpanContext = root.getSpanContext();
      assertEquals(Optional.of(rootSpanContext), spanTracker.getActiveSpan(currentThreadId()));
    }
  }

  @Test
  void trackActiveSpanForMultipleTraces() throws Exception {
    var span1 = Span.wrap(Snapshotting.spanContext().build());
    var span2 = Span.wrap(Snapshotting.spanContext().build());
    registry.register(span1.getSpanContext());
    registry.register(span2.getSpanContext());

    var executor = Executors.newFixedThreadPool(2);
    try (var scope1 = executor.submit(attach(span1)).get();
        var scope2 = executor.submit(attach(span2)).get()) {
      assertEquals(
          Optional.of(span1.getSpanContext()), spanTracker.getActiveSpan(scope1.getThreadId()));
      assertEquals(
          Optional.of(span2.getSpanContext()), spanTracker.getActiveSpan(scope2.getThreadId()));
    } finally {
      executor.shutdown();
    }
  }

  @Test
  void trackMultipleActiveSpansForSameTraceFromDifferentThreads() throws Exception {
    var traceId = IdGenerator.random().generateTraceId();
    var span1 = Span.wrap(Snapshotting.spanContext().withTraceId(traceId).build());
    var span2 = Span.wrap(Snapshotting.spanContext().withTraceId(traceId).build());
    registry.register(span1.getSpanContext());
    registry.register(span2.getSpanContext());

    var executor = Executors.newFixedThreadPool(2);
    try (var scope1 = executor.submit(attach(span1)).get();
        var scope2 = executor.submit(attach(span2)).get()) {
      assertEquals(
          Optional.of(span1.getSpanContext()), spanTracker.getActiveSpan(scope1.getThreadId()));
      assertEquals(
          Optional.of(span2.getSpanContext()), spanTracker.getActiveSpan(scope2.getThreadId()));
    } finally {
      executor.shutdown();
    }
  }

  private Callable<ThreadScope> attach(Span span) {
    return () -> {
      var context = Context.root().with(span);
      return new ThreadScope(spanTracker.attach(context), Thread.currentThread().getId());
    };
  }

  private static class ThreadScope implements Scope {
    private final Scope scope;
    private final long threadId;

    private ThreadScope(Scope scope, long threadId) {
      this.scope = scope;
      this.threadId = threadId;
    }

    public long getThreadId() {
      return threadId;
    }

    @Override
    public void close() {
      scope.close();
    }
  }

  @Test
  void activeSpanForThreadIsUnchangedWhenTraceStartsSpanInAnotherThread() throws Exception {
    var traceId = IdGenerator.random().generateTraceId();
    var root = Span.wrap(Snapshotting.spanContext().withTraceId(traceId).build());
    var child = Span.wrap(Snapshotting.spanContext().withTraceId(traceId).build());
    registry.register(root.getSpanContext());
    registry.register(child.getSpanContext());

    var executor = Executors.newSingleThreadExecutor();
    try (var scope1 = attach(root).call();
        var scope2 = executor.submit(attach(child)).get()) {
      assertEquals(
          Optional.of(root.getSpanContext()), spanTracker.getActiveSpan(scope1.getThreadId()));
      assertEquals(
          Optional.of(child.getSpanContext()), spanTracker.getActiveSpan(scope2.getThreadId()));
    } finally {
      executor.shutdown();
    }
  }

  @Test
  void doNotTrackSpanWhenNoSpanPresentInContext() {
    var context = Context.root().with(ContextKey.named("test-key"), "value");
    try (var ignored = spanTracker.attach(context)) {
      assertEquals(Optional.empty(), spanTracker.getActiveSpan(currentThreadId()));
    }
  }

  @Test
  void doNotTrackSpanWhenSpanIsNotSampled() {
    var span = Span.wrap(Snapshotting.spanContext().unsampled().build());
    var context = Context.root().with(span);
    registry.register(span.getSpanContext());

    try (var ignored = spanTracker.attach(context)) {
      assertEquals(Optional.empty(), spanTracker.getActiveSpan(currentThreadId()));
    }
  }

  @Test
  void doNotTrackSpanTraceIsNotRegisteredForSnapshotting() {
    registry.toggle(TogglableTraceRegistry.State.OFF);

    var span = Span.wrap(Snapshotting.spanContext().build());
    var context = Context.root().with(span);

    try (var ignored = spanTracker.attach(context)) {
      assertEquals(Optional.empty(), spanTracker.getActiveSpan(currentThreadId()));
    }
  }

  @Test
  void doNotTrackInvalidSpans() {
    var span = Span.wrap(SpanContext.getInvalid());
    var context = Context.root().with(span);
    registry.register(span.getSpanContext());

    try (var ignored = spanTracker.attach(context)) {
      assertEquals(Optional.empty(), spanTracker.getActiveSpan(currentThreadId()));
    }
  }

  @Test
  void doNotTrackContinuallyTrackSameSpan() {
    var span = Span.wrap(Snapshotting.spanContext().build());
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

  private long currentThreadId() {
    return Thread.currentThread().getId();
  }
}
