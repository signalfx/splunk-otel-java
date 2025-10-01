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

import com.splunk.opentelemetry.profiler.snapshot.registry.TogglableTraceRegistry;
import com.splunk.opentelemetry.profiler.snapshot.registry.TogglableTraceRegistry.State;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class TraceProfilingTest {
  private final TogglableTraceRegistry registry = new TogglableTraceRegistry();
  private final ObservableStackTraceSampler sampler = new ObservableStackTraceSampler();
  private final SnapshotProfilingSdkCustomizer customizer =
      Snapshotting.customizer().with(registry).with(sampler).build();

  @RegisterExtension
  public final OpenTelemetrySdkExtension sdk =
      OpenTelemetrySdkExtension.configure()
          .withProperty("splunk.snapshot.profiler.enabled", "true")
          .with(customizer)
          .build();

  @Test
  void startTraceProfilingWhenRootSpanContextBegins(Tracer tracer) {
    try (var ignored = Context.root().with(Volume.HIGHEST).makeCurrent()) {
      var span = tracer.spanBuilder("root").startSpan();
      try (var ignored2 = span.makeCurrent()) {
        assertThat(sampler.isBeingSampled(span.getSpanContext())).isTrue();
      }
    }
  }

  @Test
  void doNotStartTraceProfilingWhenTraceHasNotBeenSelectedForSnapshotting(Tracer tracer) {
    try (var ignored = Context.root().makeCurrent()) {
      var span = tracer.spanBuilder("root").startSpan();
      assertThat(sampler.isBeingSampled(span.getSpanContext())).isFalse();
    }
  }

  @Test
  void startTraceProfilingWhenDownstreamSpanContextBegins(Tracer tracer) {
    var remoteParentSpanContext = Snapshotting.spanContext().remote().build();
    var parentSpan = Span.wrap(remoteParentSpanContext);

    try (var ignored = Context.root().with(Volume.HIGHEST).with(parentSpan).makeCurrent()) {
      var span = tracer.spanBuilder("root").startSpan();
      try (var ignored2 = span.makeCurrent()) {
        assertThat(sampler.isBeingSampled(span.getSpanContext())).isTrue();
      }
    }
  }

  @Test
  void doNotStartTraceProfilingInDownstreamServicesWhenTraceHasNotBeenSelected(Tracer tracer) {
    var remoteParentSpanContext = Snapshotting.spanContext().remote().build();
    var parentSpan = Span.wrap(remoteParentSpanContext);

    try (var ignored = Context.root().with(parentSpan).makeCurrent()) {
      var span = tracer.spanBuilder("root").startSpan();
      assertThat(sampler.isBeingSampled(span.getSpanContext())).isFalse();
    }
  }

  @Test
  void stopTraceProfilingWhenEntrySpanEnds(Tracer tracer) {
    try (var ignored = Context.root().with(Volume.HIGHEST).makeCurrent()) {
      var entry = tracer.spanBuilder("entry").startSpan();
      entry.end();
      assertThat(sampler.isBeingSampled(entry.getSpanContext())).isFalse();
    }
  }

  @Test
  void canStopTraceProfilingFromDifferentThread(Tracer tracer) throws Exception {
    try (var ignored = Context.root().with(Volume.HIGHEST).makeCurrent()) {
      var entry = tracer.spanBuilder("entry").startSpan();

      var endingThread = new Thread(entry::end);
      endingThread.start();
      endingThread.join();

      assertThat(sampler.isBeingSampled(entry.getSpanContext())).isFalse();
    }
  }

  @Test
  void doNotStartTraceProfilingWhenTraceIsNotRegistered(Tracer tracer) {
    registry.toggle(State.OFF);

    try (var ignored = Context.root().with(Volume.HIGHEST).makeCurrent()) {
      var root = tracer.spanBuilder("root").setSpanKind(SpanKind.SERVER).startSpan();
      var client =
          tracer
              .spanBuilder("client")
              .setSpanKind(SpanKind.CLIENT)
              .setParent(Context.current().with(root))
              .startSpan();
      tracer.spanBuilder("span").setParent(Context.current().with(client)).startSpan();
      assertThat(sampler.isBeingSampled(root.getSpanContext())).isFalse();
    }
  }

  @Test
  void onlyStopTraceProfilingWhenEntrySpanContextCloses(Tracer tracer) {
    try (var ignored = Context.root().with(Volume.HIGHEST).makeCurrent()) {
      var root = tracer.spanBuilder("root").setSpanKind(SpanKind.SERVER).startSpan();
      try (var ignoredRootContext = root.makeCurrent()) {
        var child =
            tracer
                .spanBuilder("child")
                .setSpanKind(SpanKind.CLIENT)
                .setParent(Context.current().with(root))
                .startSpan();
        child.end();
        assertThat(sampler.isBeingSampled(root.getSpanContext())).isTrue();
      }
    }
  }
}
