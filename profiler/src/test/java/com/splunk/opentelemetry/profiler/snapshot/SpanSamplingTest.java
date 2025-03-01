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

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;

class SpanSamplingTest {
  private final TraceRegistry registry = new TraceRegistry();
  private final SnapshotProfilingSdkCustomizer customizer =
      Snapshotting.customizer().with(registry).build();

  @Nested
  class SpanSamplingDisabled {
    @RegisterExtension
    public final OpenTelemetrySdkExtension s =
        OpenTelemetrySdkExtension.builder()
            .withProperty("splunk.snapshot.profiler.enabled", "true")
            .withSampler(Sampler.alwaysOff())
            .with(customizer)
            .build();

    @ParameterizedTest
    @SpanKinds.Entry
    void doNotRegisterTraceForProfilingWhenSpanSamplingIsOff(SpanKind kind, Tracer tracer) {
      try (var ignored = Context.current().with(Volume.HIGHEST).makeCurrent()) {
        var root = tracer.spanBuilder("root").setSpanKind(kind).startSpan();
        assertThat(registry.isRegistered(root.getSpanContext())).isFalse();
      }
    }
  }

  @Nested
  class SpanSamplingEnabled {
    @RegisterExtension
    public final OpenTelemetrySdkExtension s =
        OpenTelemetrySdkExtension.builder()
            .withProperty("splunk.snapshot.profiler.enabled", "true")
            .withSampler(Sampler.alwaysOn())
            .with(customizer)
            .build();

    @ParameterizedTest
    @SpanKinds.Entry
    void registerTraceForProfilingWhenSpanSamplingIsOn(SpanKind kind, Tracer tracer) {
      try (var ignored = Context.current().with(Volume.HIGHEST).makeCurrent()) {
        var root = tracer.spanBuilder("root").setSpanKind(kind).startSpan();
        assertThat(registry.isRegistered(root.getSpanContext())).isTrue();
      }
    }
  }
}
