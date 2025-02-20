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

import com.splunk.opentelemetry.profiler.snapshot.SnapshotSnapAttributeTest.TogglableTraceRegistry.State;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;

class SnapshotSnapAttributeTest {
  private final TogglableTraceRegistry registry = new TogglableTraceRegistry();
  private final SnapshotProfilingSdkCustomizer customizer =
      new SnapshotProfilingSdkCustomizer(registry);

  @RegisterExtension
  public final OpenTelemetrySdkExtension s =
      OpenTelemetrySdkExtension.builder()
          .with(customizer)
          .withProperty("splunk.snapshot.profiler.enabled", "true")
          .build();

  @ParameterizedTest
  @SpanKinds.Entry
  void addSnapshotSpanAttributeToEntrySpans(SpanKind kind, Tracer tracer) {
    var span = (ReadWriteSpan) tracer.spanBuilder("root").setSpanKind(kind).startSpan();
    try (var ignored = span.makeCurrent()) {
      var attribute = span.getAttribute(AttributeKey.booleanKey("splunk.snapshot.profiling"));
      assertThat(attribute).isTrue();
    }
  }

  @ParameterizedTest
  @SpanKinds.NonEntry
  void onlyRegisterTraceForProfilingWhenRootSpanIsEntrySpan(SpanKind kind, Tracer tracer) {
    var span = (ReadWriteSpan) tracer.spanBuilder("root").setSpanKind(kind).startSpan();
    try (var ignored = span.makeCurrent()) {
      var attribute = span.getAttribute(AttributeKey.booleanKey("splunk.snapshot.profiling"));
      assertThat(attribute).isNull();
    }
  }

  @ParameterizedTest
  @SpanKinds.Entry
  void addSnapshotSpanAttributeToAllEntrySpans(SpanKind kind, Tracer tracer) {
    try (var root = tracer.spanBuilder("root").setSpanKind(kind).startSpan().makeCurrent()) {
      try (var client =
          tracer.spanBuilder("client").setSpanKind(SpanKind.CLIENT).startSpan().makeCurrent()) {
        var span = (ReadWriteSpan) tracer.spanBuilder("root").setSpanKind(kind).startSpan();
        try (var ignored = span.makeCurrent()) {
          var attribute = span.getAttribute(AttributeKey.booleanKey("splunk.snapshot.profiling"));
          assertThat(attribute).isTrue();
        }
      }
    }
  }

  @ParameterizedTest
  @SpanKinds.Entry
  void doNotAddSnapshotSpanAttributeWhenTraceIsNotRegisteredForSnapshotting(
      SpanKind kind, Tracer tracer) {
    registry.toggle(State.OFF);

    var span = (ReadWriteSpan) tracer.spanBuilder("root").setSpanKind(kind).startSpan();
    try (var ignored = span.makeCurrent()) {
      var attribute = span.getAttribute(AttributeKey.booleanKey("splunk.snapshot.profiling"));
      assertThat(attribute).isNull();
    }
  }

  static class TogglableTraceRegistry extends TraceRegistry {
    enum State {
      ON,
      OFF
    }

    private State state = State.ON;

    @Override
    public void register(SpanContext spanContext) {
      if (state == State.ON) {
        super.register(spanContext);
      }
    }

    public void toggle(State state) {
      this.state = state;
    }

    @Override
    public boolean isRegistered(SpanContext spanContext) {
      return super.isRegistered(spanContext);
    }

    @Override
    public void unregister(SpanContext spanContext) {
      super.unregister(spanContext);
    }
  }
}
