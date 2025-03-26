package com.splunk.opentelemetry.profiler.snapshot;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;

class ActiveSpanTracker implements ContextStorage, SpanTracker {
  static final ActiveSpanTracker INSTANCE = new ActiveSpanTracker();

  private static ContextStorage DELEGATE;

  static void configure(ContextStorage contextStorage) {
    DELEGATE = contextStorage;
  }

  @VisibleForTesting
  static void reset() {
    DELEGATE = null;
  }

  static boolean isReady() {
    return DELEGATE != null;
  }

  private final ConcurrentMap<String, SpanContext> activeSpans = new ConcurrentHashMap<>();

  private TraceRegistry registry;

  void configure(TraceRegistry registry) {
    this.registry = registry;
  }

  @Override
  public Scope attach(Context toAttach) {
    if (!isReady()) {
      return Scope.noop();
    }

    Scope scope = DELEGATE.attach(toAttach);
    if (registry == null) {
      return scope;
    }

    Span span = Span.fromContext(toAttach);
    if (doNotTrack(span)) {
      return scope;
    }

    SpanContext spanContext = span.getSpanContext();
    String traceId = spanContext.getTraceId();
    SpanContext current =  activeSpans.get(traceId);
    if (current == spanContext) {
      return scope;
    }

    SpanContext previous = activeSpans.put(traceId, spanContext);
    return () -> {
      activeSpans.computeIfPresent(traceId, (id, sc) -> previous);
      scope.close();
    };
  }

  private boolean doNotTrack(Span  span) {
    if (!span.isRecording()) {
      return true;
    }
    SpanContext  spanContext = span.getSpanContext();
    return !registry.isRegistered(spanContext);
  }

  @Nullable
  @Override
  public Context current() {
    return DELEGATE.current();
  }

  @Override
  public Optional<SpanContext> getActiveSpan(String traceId) {
    return Optional.ofNullable(activeSpans.get(traceId));
  }

  private ActiveSpanTracker() {}
}
