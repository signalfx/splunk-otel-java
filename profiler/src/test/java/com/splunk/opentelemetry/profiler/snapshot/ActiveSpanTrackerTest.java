package com.splunk.opentelemetry.profiler.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ActiveSpanTrackerTest {
  private final TestStorage contextStorage = new TestStorage();
  private final ToggleableSampler sampler = new ToggleableSampler();
  private final TogglableTraceRegistry registry = new TogglableTraceRegistry();
  private final ActiveSpanTracker spanTracker = ActiveSpanTracker.INSTANCE;

  @RegisterExtension final OpenTelemetrySdkExtension s = OpenTelemetrySdkExtension.builder()
      .withSampler(sampler)
      .build();

  @BeforeEach
  void setup() {
    ActiveSpanTracker.configure(contextStorage);
    spanTracker.configure(registry);
  }

  @Test
  void currentContextComesFromOpenTelemetryContextStorage() {
    var context = Context.root().with(ContextKey.named("test-key"), "value");
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
  void trackActiveSpanWhenNewContextAttached(Tracer tracer) {
    var span = tracer.spanBuilder("root").startSpan();
    var spanContext = span.getSpanContext();
    var context = Context.root().with(span);
    registry.register(spanContext);

    try (var ignored = spanTracker.attach(context)) {
      var traceId = span.getSpanContext().getTraceId();
      assertEquals(Optional.of(spanContext), spanTracker.getActiveSpan(traceId));
    }
  }

  @Test
  void trackActiveSpanAfterAcrossMultipleContextChanges(Tracer tracer) {
    var root = tracer.spanBuilder("root").startSpan();
    var context = Context.root().with(root);
    registry.register(root.getSpanContext());

    try (var ignored = spanTracker.attach(context)) {
      var span = tracer.spanBuilder("child").setParent(context).startSpan();
      var spanContext = span.getSpanContext();
      context = context.with(span);
      try (var i = spanTracker.attach(context)) {
        var traceId = span.getSpanContext().getTraceId();
        assertEquals(Optional.of(spanContext), spanTracker.getActiveSpan(traceId));
      }
    }
  }

  @Test
  void noActiveSpanForTraceAfterSpansScopeIsClosed(Tracer tracer) {
    var span = tracer.spanBuilder("root").startSpan();
    var spanContext = span.getSpanContext();
    var context = Context.root().with(span);
    registry.register(spanContext);

    var scope = spanTracker.attach(context);
    scope.close();

    assertEquals(Optional.empty(), spanTracker.getActiveSpan(spanContext.getTraceId()));
  }

  @Test
  void trackActiveSpanForMultipleTraces(Tracer tracer) throws Exception {
    var executor = Executors.newSingleThreadExecutor();
    var root1 = tracer.spanBuilder("root").startSpan();
    var root2 = tracer.spanBuilder("root").startSpan();
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
      var context = Context.root().with(span);
      return spanTracker.attach(context);
    };
  }

  @Test
  void doNotTrackSpanWhenNoSpanPresentInContext() {
    var context = Context.root().with(ContextKey.named("test-key"), "value");
    try (var scope = spanTracker.attach(context)) {
      assertEquals(Optional.empty(), spanTracker.getActiveSpan(TraceId.getInvalid()));
      assertInstanceOf(TestScope.class, scope);
    }
  }

  @Test
  void doNotTrackSpanWhenSpanIsNotSampled(Tracer tracer) {
    sampler.toggle(ToggleableSampler.State.OFF);

    var root = tracer.spanBuilder("root").startSpan();
    var context = Context.root().with(root);
    registry.register(root.getSpanContext());

    try (var scope = spanTracker.attach(context)) {
      var traceId = root.getSpanContext().getTraceId();
      assertEquals(Optional.empty(), spanTracker.getActiveSpan(traceId));
      assertInstanceOf(TestScope.class, scope);
    }
  }

  @Test
  void doNotTrackSpanTraceIsNotRegisteredForSnapshotting(Tracer tracer) {
    registry.toggle(TogglableTraceRegistry.State.OFF);

    var root = tracer.spanBuilder("root").startSpan();
    var context = Context.root().with(root);

    try (var scope = spanTracker.attach(context)) {
      var traceId = root.getSpanContext().getTraceId();
      assertEquals(Optional.empty(), spanTracker.getActiveSpan(traceId));
      assertInstanceOf(TestScope.class, scope);
    }
  }

  @Test
  void doNotTrackContinuallyTrackSameSpan(Tracer tracer) {
    var storage = new TestStorage();
    ActiveSpanTracker.configure(storage);
    var root = tracer.spanBuilder("root").startSpan();
    var context = Context.root().with(root);
    registry.register(root.getSpanContext());

    try (var ignored = spanTracker.attach(context)) {
      var newContext = context.with(ContextKey.named("test-key"), "value");
      try (var scope = spanTracker.attach(newContext)) {
        assertInstanceOf(TestScope.class, scope);
      }
    }
  }

  private static class ToggleableSampler implements Sampler {
    enum State {
      ON,
      OFF
    }

    private State state = State.ON;

    void toggle(State state) {
      this.state = state;
    }

    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name,
        SpanKind spanKind, Attributes attributes, List<LinkData> parentLinks) {
      if (state == State.OFF) {
        return SamplingResult.drop();
      }
      return SamplingResult.recordAndSample();
    }

    @Override
    public String getDescription() {
      return getClass().getSimpleName();
    }
  }

  private static class TestStorage implements ContextStorage {
    private final ThreadLocal<Context> contexts = new ThreadLocal<>();

    @Override
    public Scope attach(Context toAttach) {
      var previousContext = contexts.get();
      store(toAttach);
      return new TestScope(this, previousContext);
    }

    @Nullable
    @Override
    public Context current() {
      return contexts.get();
    }

    void store(Context context) {
      contexts.set(context);
    }
  }

  private static class TestScope implements Scope {
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
