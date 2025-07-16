package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import java.util.function.Supplier;
import javax.annotation.Nullable;

class TraceThreadChangeDetector implements ContextStorage {
  private final ContextStorage delegate;
  private final TraceRegistry registry;
  private final Supplier<StackTraceSampler> sampler;

  TraceThreadChangeDetector(ContextStorage delegate,  TraceRegistry registry, Supplier<StackTraceSampler> sampler) {
    this.delegate = delegate;
    this.registry = registry;
    this.sampler = sampler;
  }

  @Override
  public Scope attach(Context toAttach) {
    Scope scope = delegate.attach(toAttach);
    SpanContext newSpanContext = Span.fromContext(toAttach).getSpanContext();
    if (doNotTrack(newSpanContext)) {
      return scope;
    }

    Thread thread = Thread.currentThread();
    if (sampler.get().isBeingSampled(thread)) {
      return scope;
    }

    sampler.get().start(thread, newSpanContext);
    return () -> {
      sampler.get().stop(thread, newSpanContext);
      scope.close();;
    };
  }

  private boolean doNotTrack(SpanContext spanContext) {
    return !spanContext.isSampled() || !registry.isRegistered(spanContext);
  }

  @Nullable
  @Override
  public Context current() {
    return delegate.current();
  }
}
