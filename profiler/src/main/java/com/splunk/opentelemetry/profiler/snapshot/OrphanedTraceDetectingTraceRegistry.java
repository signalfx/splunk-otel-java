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

import io.opentelemetry.api.trace.SpanContext;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

class OrphanedTraceDetectingTraceRegistry implements TraceRegistry {
  private final Set<Key> traces = Collections.newSetFromMap(new ConcurrentHashMap<>());
  private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();

  private final TraceRegistry delegate;
  private final Supplier<StackTraceSampler> sampler;
  private final Thread thread;

  OrphanedTraceDetectingTraceRegistry(TraceRegistry delegate, Supplier<StackTraceSampler> sampler) {
    this.delegate = delegate;
    this.sampler = sampler;

    thread = new Thread(this::unregisterOrphanedTraces);
    thread.setName("orphaned-trace-detector");
    thread.setDaemon(true);
    thread.start();
  }

  @Override
  public void register(SpanContext spanContext) {
    delegate.register(spanContext);
    traces.add(new WeakSpanContext(spanContext, referenceQueue));
  }

  @Override
  public boolean isRegistered(SpanContext spanContext) {
    return delegate.isRegistered(spanContext);
  }

  @Override
  public void unregister(SpanContext spanContext) {
    delegate.unregister(spanContext);
    traces.remove(new LookupKey(spanContext));
  }

  public void unregisterOrphanedTraces() {
    while (!Thread.interrupted()) {
      try {
        Object reference = referenceQueue.remove();
        if (reference != null) {
          Key key = (Key) reference;
          traces.remove(key);
          delegate.unregister(key.getSpanContext());
          sampler.get().stop(key.getSpanContext());
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public void close() {
    delegate.close();
    thread.interrupt();
  }

  interface Key {
    SpanContext getSpanContext();
  }

  private static class LookupKey implements Key {
    private final SpanContext spanContext;

    private LookupKey(SpanContext spanContext) {
      this.spanContext = spanContext;
    }

    @Override
    public SpanContext getSpanContext() {
      return spanContext;
    }
  }

  private static class WeakSpanContext extends WeakReference<SpanContext> implements Key {
    private final SpanContext spanContext;

    public WeakSpanContext(SpanContext referent, ReferenceQueue<Object> q) {
      super(referent, q);
      this.spanContext =
          SpanContext.create(
              referent.getTraceId(),
              referent.getSpanId(),
              referent.getTraceFlags(),
              referent.getTraceState());
    }

    @Override
    public SpanContext getSpanContext() {
      return spanContext;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(spanContext);
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      WeakSpanContext that = (WeakSpanContext) o;
      return Objects.equals(spanContext, that.spanContext);
    }
  }
}
