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
import java.io.Closeable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Clear snapshot profiling info for traces where root span was not ended. If there is a bug in the
 * instrumentation that causes the root span that triggered profiling not to be ended we'll end up
 * leaking the trace id in the trace registry and the stack sampler will continue running. To reduce
 * the impact of such instrumentation bugs this class triggers cleanup when the root span for which
 * the profiling was started is garbage collected.
 */
class OrphanedTraceCleaner implements Closeable {
  private final Set<Key> traces = Collections.newSetFromMap(new ConcurrentHashMap<>());
  private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();

  private final TraceRegistry traceRegistry;
  private final Supplier<StackTraceSampler> sampler;
  private final Thread thread;

  OrphanedTraceCleaner(TraceRegistry traceRegistry, Supplier<StackTraceSampler> sampler) {
    this.traceRegistry = traceRegistry;
    this.sampler = sampler;

    thread = new Thread(this::unregisterOrphanedTraces);
    thread.setName("orphaned-trace-cleaner");
    thread.setDaemon(true);
    thread.start();
  }

  void register(Span span) {
    traces.add(new WeakKey(span, referenceQueue));
  }

  void unregister(SpanContext spanContext) {
    traces.remove(new LookupKey(spanContext));
  }

  // visible for tests
  int getTracesSize() {
    return traces.size();
  }

  private void unregisterOrphanedTraces() {
    try {
      while (!Thread.interrupted()) {
        Object reference = referenceQueue.remove();
        if (reference != null) {
          Key key = (Key) reference;
          if (traces.remove(key)) {
            traceRegistry.unregister(key.getSpanContext().getTraceId());
            sampler.get().stopAllSampling(key.getSpanContext());
          }
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void close() {
    thread.interrupt();
  }

  private interface Key {
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

    @Override
    public int hashCode() {
      return Objects.hash(spanContext);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Key)) {
        return false;
      }
      Key key = (Key) obj;
      return Objects.equals(spanContext, key.getSpanContext());
    }
  }

  private static class WeakKey extends WeakReference<Span> implements Key {
    private final SpanContext spanContext;

    public WeakKey(Span referent, ReferenceQueue<Object> queue) {
      super(referent, queue);
      this.spanContext = referent.getSpanContext();
    }

    @Override
    public SpanContext getSpanContext() {
      return spanContext;
    }

    @Override
    public int hashCode() {
      return Objects.hash(spanContext);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Key)) {
        return false;
      }
      Key key = (Key) obj;
      return Objects.equals(spanContext, key.getSpanContext());
    }
  }
}
