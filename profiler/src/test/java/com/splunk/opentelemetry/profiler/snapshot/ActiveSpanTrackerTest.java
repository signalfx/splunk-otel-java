package com.splunk.opentelemetry.profiler.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
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
  private final TestStorage contextStorage = new TestStorage();
  private final TogglableTraceRegistry registry = new TogglableTraceRegistry();
  private final ActiveSpanTracker spanTracker = new ActiveSpanTracker(contextStorage, registry);

  @Test
  void currentContextComesFromOpenTelemetryContextStorage() {
    var context = contextStorage.root().with(ContextKey.named("test-key"), "value");
    try (var ignored = spanTracker.attach(context)) {
      assertEquals(contextStorage.current(), spanTracker.current());
    }
  }

  @Test
  void noActiveSpanBeforeFirstSpanInTraceIsAttachedToContext() {
    var traceId = IdGenerator.random().generateTraceId();
    assertEquals(Optional.empty(), spanTracker.getActiveSpan(traceId));
  }

  @Test
  void trackActiveSpanWhenNewContextAttached() {
    var span = FakeSpan.randomSpan();
    var spanContext = span.getSpanContext();
    var context = contextStorage.root().with(span);
    registry.register(spanContext);

    try (var ignored = spanTracker.attach(context)) {
      var traceId = span.getSpanContext().getTraceId();
      assertEquals(Optional.of(spanContext), spanTracker.getActiveSpan(traceId));
    }
  }

  @Test
  void trackActiveSpanAfterAcrossMultipleContextChanges() {
    var root = FakeSpan.randomSpan();
    var context = contextStorage.root().with(root);
    registry.register(root.getSpanContext());

    try (var ignored = spanTracker.attach(context)) {
      var span = FakeSpan.newSpan(root.getSpanContext());
      var spanContext = span.getSpanContext();
      context = context.with(span);
      try (var i = spanTracker.attach(context)) {
        var traceId = span.getSpanContext().getTraceId();
        assertEquals(Optional.of(spanContext), spanTracker.getActiveSpan(traceId));
      }
    }
  }

  @Test
  void noActiveSpanForTraceAfterSpansScopeIsClosed() {
    var span = FakeSpan.randomSpan();
    var spanContext = span.getSpanContext();
    var context = contextStorage.root().with(span);
    registry.register(spanContext);

    var scope = spanTracker.attach(context);
    scope.close();

    assertEquals(Optional.empty(), spanTracker.getActiveSpan(spanContext.getTraceId()));
  }

  @Test
  void trackActiveSpanForMultipleTraces() throws Exception {
    var executor = Executors.newSingleThreadExecutor();
    var root1 = FakeSpan.randomSpan();
    var root2 = FakeSpan.randomSpan();
    registry.register(root1.getSpanContext());
    registry.register(root2.getSpanContext());

    try (var scope1 = executor.submit(attach(root1)).get();
        var scope2 = executor.submit(attach(root2)).get()) {
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
      var context = contextStorage.root().with(span);
      return spanTracker.attach(context);
    };
  }

  @Test
  void doNotTrackSpanWhenNoSpanPresentInContext() {
    var context = contextStorage.root().with(ContextKey.named("test-key"), "value");
    try (var scope = spanTracker.attach(context)) {
      assertEquals(Optional.empty(), spanTracker.getActiveSpan(TraceId.getInvalid()));
      assertInstanceOf(TestStorage.TestScope.class, scope);
    }
  }

  @Test
  void doNotTrackSpanWhenSpanIsNotSampled() {
    var span = FakeSpan.randomSpan();
    span.stopRecording();
    var context = contextStorage.root().with(span);
    registry.register(span.getSpanContext());

    try (var scope = spanTracker.attach(context)) {
      var traceId = span.getSpanContext().getTraceId();
      assertEquals(Optional.empty(), spanTracker.getActiveSpan(traceId));
      assertInstanceOf(TestStorage.TestScope.class, scope);
    }
  }

  @Test
  void doNotTrackSpanTraceIsNotRegisteredForSnapshotting() {
    registry.toggle(TogglableTraceRegistry.State.OFF);

    var span = FakeSpan.randomSpan();
    var context = contextStorage.root().with(span);

    try (var scope = spanTracker.attach(context)) {
      var traceId = span.getSpanContext().getTraceId();
      assertEquals(Optional.empty(), spanTracker.getActiveSpan(traceId));
      assertInstanceOf(TestStorage.TestScope.class, scope);
    }
  }

  @Test
  void doNotTrackContinuallyTrackSameSpan() {
    var span = FakeSpan.randomSpan();
    var context = contextStorage.root().with(span);
    registry.register(span.getSpanContext());

    try (var ignored = spanTracker.attach(context)) {
      var newContext = context.with(ContextKey.named("test-key"), "value");
      try (var scope = spanTracker.attach(newContext)) {
        assertInstanceOf(TestStorage.TestScope.class, scope);
      }
    }
  }

  private static class TestStorage implements ContextStorage {
    private final ThreadLocal<Context> contexts = new ThreadLocal<>();

    @Override
    public TestScope attach(Context toAttach) {
      var previousContext = contexts.get();
      store(toAttach);
      return new TestScope(this, previousContext);
    }

    @Override
    public Context current() {
      return contexts.get();
    }

    void store(Context context) {
      contexts.set(context);
    }

    static class TestScope implements Scope {
      private final TestStorage storage;
      private final Context beforeAttach;

      private TestScope(TestStorage storage, Context beforeAttach) {
        this.storage = storage;
        this.beforeAttach = beforeAttach;
      }

      @Override
      public void close() {
        storage.store(beforeAttach);
      }
    }
  }

  private static class FakeSpan implements Span {
    public static FakeSpan randomSpan() {
      return newSpan(IdGenerator.random().generateTraceId());
    }

    public static FakeSpan newSpan(SpanContext parentSpanContext) {
      return newSpan(parentSpanContext.getTraceId());
    }

    public static FakeSpan newSpan(String traceId) {
      return new FakeSpan(SpanContext.create(traceId, IdGenerator.random().generateSpanId(), TraceFlags.getDefault(), TraceState.getDefault()));
    }

    private final SpanContext spanContext;
    private boolean recording = true;

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

    void stopRecording() {
      recording = false;
    }

    @Override
    public boolean isRecording() {
      return recording;
    }
  }
}
