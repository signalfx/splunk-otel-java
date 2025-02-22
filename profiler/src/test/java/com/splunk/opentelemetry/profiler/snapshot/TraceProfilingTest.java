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

import com.splunk.opentelemetry.profiler.snapshot.TogglableTraceRegistry.State;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;

class TraceProfilingTest {
  private final TogglableTraceRegistry registry = new TogglableTraceRegistry();
  private final ObservableStackTraceSampler sampler = new ObservableStackTraceSampler();
  private final SnapshotProfilingSdkCustomizer customizer =
      Profiling.customizer().with(registry).with(sampler).build();

  @RegisterExtension
  public final OpenTelemetrySdkExtension sdk =
      OpenTelemetrySdkExtension.builder()
          .withProperty("splunk.snapshot.profiler.enabled", "true")
          .with(customizer)
          .build();

  @ParameterizedTest
  @SpanKinds.Entry
  void startTraceProfilingWhenEntrySpanStarts(SpanKind kind, Tracer tracer) {
    try (var ignored = Context.root().with(Volume.HIGHEST).makeCurrent()) {
      tracer.spanBuilder("root").setSpanKind(kind).startSpan();
      assertThat(sampler.isBeingSampled(Thread.currentThread().getId())).isTrue();
    }
  }

  @ParameterizedTest
  @SpanKinds.Entry
  void stopTraceProfilingWhenEntrySpanEnds(SpanKind kind, Tracer tracer) {
    try (var ignored = Context.root().with(Volume.HIGHEST).makeCurrent()) {
      var entry = tracer.spanBuilder("entry").setSpanKind(kind).startSpan();
      entry.end();
      assertThat(sampler.isBeingSampled(Thread.currentThread().getId())).isFalse();
    }
  }

  @ParameterizedTest
  @SpanKinds.NonEntry
  void doNotStartTraceProfilingForNonEntrySpanTypes(SpanKind kind, Tracer tracer) {
    try (var ignored = Context.root().with(Volume.HIGHEST).makeCurrent()) {
      tracer.spanBuilder("span").setSpanKind(kind).startSpan();
      assertThat(sampler.isBeingSampled(Thread.currentThread().getId())).isFalse();
    }
  }

  @ParameterizedTest
  @SpanKinds.Entry
  void doNotStartTraceProfilingWhenTraceIsNotRegistered(SpanKind kind, Tracer tracer) {
    registry.toggle(State.OFF);

    try (var ignored = Context.root().with(Volume.HIGHEST).makeCurrent()) {
      var root = tracer.spanBuilder("root").setSpanKind(SpanKind.SERVER).startSpan();
      var client =
          tracer
              .spanBuilder("client")
              .setSpanKind(SpanKind.CLIENT)
              .setParent(Context.current().with(root))
              .startSpan();
      tracer
          .spanBuilder("span")
          .setSpanKind(kind)
          .setParent(Context.current().with(client))
          .startSpan();
      assertThat(sampler.isBeingSampled(Thread.currentThread().getId())).isFalse();
    }
  }

  @ParameterizedTest
  @SpanKinds.NonEntry
  void onlyStopTraceProfilingWhenEntrySpanEnds(SpanKind kind, Tracer tracer) {
    try (var ignored = Context.root().with(Volume.HIGHEST).makeCurrent()) {
      var root = tracer.spanBuilder("root").setSpanKind(SpanKind.SERVER).startSpan();
      var child =
          tracer
              .spanBuilder("child")
              .setSpanKind(kind)
              .setParent(Context.current().with(root))
              .startSpan();
      child.end();
      assertThat(sampler.isBeingSampled(Thread.currentThread().getId())).isTrue();
    }
  }
}
