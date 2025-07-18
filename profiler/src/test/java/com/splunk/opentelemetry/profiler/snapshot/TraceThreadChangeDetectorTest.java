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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class TraceThreadChangeDetectorTest {
  private final ContextStorage storage = ContextStorage.get();
  private final TraceRegistry registry = new TraceRegistry();
  private final ObservableStackTraceSampler sampler = new ObservableStackTraceSampler();
  private final TraceThreadChangeDetector detector =
      new TraceThreadChangeDetector(storage, registry, () -> sampler);

  @Test
  void currentContextComesFromOpenTelemetryContextStorage() {
    var context = Context.root().with(ContextKey.named("test-key"), "value");
    try (var ignored = detector.attach(context)) {
      assertEquals(storage.current(), detector.current());
    }
  }

  @Test
  void startSamplingThreadWhenSampledContextSwitchesToNewThread() throws Exception {
    var spanContext = Snapshotting.spanContext().build();
    registry.register(spanContext);
    sampler.start(Thread.currentThread(), spanContext);

    var span = Span.wrap(spanContext);
    var context = Context.root().with(span);

    var executor = Executors.newSingleThreadExecutor();
    try (var ts = executor.submit(captureThread(() -> detector.attach(context))).get()) {
      assertThat(sampler.isBeingSampled(ts.thread)).isTrue();
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void stopSamplingThreadWhenScopeCloses() throws Exception {
    var spanContext = Snapshotting.spanContext().build();
    registry.register(spanContext);
    sampler.start(Thread.currentThread(), spanContext);

    var span = Span.wrap(spanContext);
    var context = Context.root().with(span);

    var executor = Executors.newSingleThreadExecutor();
    try {
      var ts = executor.submit(captureThread(() -> detector.attach(context))).get();
      ts.close();
      assertThat(sampler.isBeingSampled(ts.thread)).isFalse();
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void doNotStartSamplingThreadWhenTraceNotRegisteredForSnapshotting() throws Exception {
    var spanContext = Snapshotting.spanContext().build();

    var span = Span.wrap(spanContext);
    var context = Context.root().with(span);

    var executor = Executors.newSingleThreadExecutor();
    try (var ts = executor.submit(captureThread(() -> detector.attach(context))).get()) {
      assertThat(sampler.isBeingSampled(ts.thread)).isFalse();
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void doNotStartSamplingThreadWhenSpanIsNotSampled() throws Exception {
    var spanContext = Snapshotting.spanContext().unsampled().build();
    registry.register(spanContext);

    var span = Span.wrap(spanContext);
    var context = Context.root().with(span);

    var executor = Executors.newSingleThreadExecutor();
    try (var ts = executor.submit(captureThread(() -> detector.attach(context))).get()) {
      assertThat(sampler.isBeingSampled(ts.thread)).isFalse();
    } finally {
      executor.shutdownNow();
    }
  }

  private Callable<ThreadScope> captureThread(Callable<Scope> callable) {
    return () -> new ThreadScope(Thread.currentThread(), callable.call());
  }

  private static class ThreadScope implements Scope {
    private final Thread thread;
    private final Scope scope;

    private ThreadScope(Thread thread, Scope scope) {
      this.thread = thread;
      this.scope = scope;
    }

    @Override
    public void close() {
      scope.close();
    }
  }
}
