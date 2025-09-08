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
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ContextStorageWrapperTest {
  /**
   * {@link ResettingContextStorageWrapper} extends {@link ContextStorageWrapper} so we're still
   * the intended functionality.
   */
  @RegisterExtension
  private final ResettingContextStorageWrapper wrapper = new ResettingContextStorageWrapper();

  @Test
  void installActiveSpanTracker() {
    var spanContext = Snapshotting.spanContext().build();
    var span = Span.wrap(spanContext);

    var registry = new RecordingTraceRegistry();
    registry.register(spanContext);

    wrapper.wrapContextStorage(registry);
    try (var ignored = Context.current().with(span).makeCurrent()) {
      var activeSpan = SpanTracker.SUPPLIER.get().getActiveSpan(Thread.currentThread());
      assertEquals(Optional.of(spanContext), activeSpan);
    }
  }

  @Test
  void installThreadChangeDetector() {
    var spanContext = Snapshotting.spanContext().build();
    var span = Span.wrap(spanContext);

    var registry = new RecordingTraceRegistry();
    registry.register(spanContext);

    var sampler = new ObservableStackTraceSampler();
    StackTraceSampler.SUPPLIER.configure(sampler);

    wrapper.wrapContextStorage(registry);
    try (var ignored = Context.current().with(span).makeCurrent()) {
      assertThat(sampler.isBeingSampled(Thread.currentThread())).isTrue();
    }
  }

  /**
   * This test implicitly tests that span tracking happens first by checking the reported
   * span id of the first on demand sample taken when profiling is started for the current
   * thread. When span id tracking happens 2nd, the reported span will be an invalid span id.
   */
  @Test
  void spanTrackingRunsBeforeThreadChangeDetector() {
    var spanContext = Snapshotting.spanContext().build();
    var span = Span.wrap(spanContext);

    var registry = new RecordingTraceRegistry();
    registry.register(spanContext);

    var staging = new InMemoryStagingArea();
    StagingArea.SUPPLIER.configure(staging);

    var sampler = new PeriodicStackTraceSampler(StagingArea.SUPPLIER, SpanTracker.SUPPLIER, Duration.ofMinutes(1));
    StackTraceSampler.SUPPLIER.configure(sampler);

    wrapper.wrapContextStorage(registry);
    try (var ignored = Context.current().with(span).makeCurrent()) {
      var activeSpan = SpanTracker.SUPPLIER.get().getActiveSpan(Thread.currentThread()).orElseThrow();
      var stackTrace = staging.allStackTraces().get(0);
      assertEquals(activeSpan.getSpanId(), stackTrace.getSpanId());
    }
  }
}
