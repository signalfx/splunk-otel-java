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

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpanSamplingTest {
  private final TraceRegistry registry = new TraceRegistry();
  private final SnapshotProfilingSdkCustomizer customizer =
      new SnapshotProfilingSdkCustomizer(registry);

  @Nested
  class SpanSamplingDisabled {
    @RegisterExtension
    public final OpenTelemetrySdkExtension s = OpenTelemetrySdkExtension.builder()
            .withProperty("splunk.snapshot.profiler.enabled", "true")
            .withSampler(Sampler.alwaysOff())
            .with(customizer)
            .build();

    @ParameterizedTest
    @EnumSource(
            value = SpanKind.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"SERVER", "CONSUMER"})
    void doNotRegisterTraceForProfilingWhenSpanSamplingIsOff(SpanKind kind, Tracer tracer) {
      var root = tracer.spanBuilder("root").setSpanKind(kind).startSpan();
      assertFalse(registry.isRegistered(root.getSpanContext()));
    }
  }

  @Nested
  class SpanSamplingEnabled {
    @RegisterExtension
    public final OpenTelemetrySdkExtension s = OpenTelemetrySdkExtension.builder()
            .withProperty("splunk.snapshot.profiler.enabled", "true")
            .withSampler(Sampler.alwaysOn())
            .with(customizer)
            .build();

    @ParameterizedTest
    @EnumSource(
            value = SpanKind.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"SERVER", "CONSUMER"})
    void registerTraceForProfilingWhenSpanSamplingIsOn(SpanKind kind, Tracer tracer) {
      var root = tracer.spanBuilder("root").setSpanKind(kind).startSpan();
      assertTrue(registry.isRegistered(root.getSpanContext()));
    }
  }
}
