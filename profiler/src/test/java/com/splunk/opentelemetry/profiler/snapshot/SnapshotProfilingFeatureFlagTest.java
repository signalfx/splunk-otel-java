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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SnapshotProfilingFeatureFlagTest {
  private final TraceRegistry registry = new TraceRegistry();
  private final SnapshotProfilingSdkCustomizer customizer =
      new SnapshotProfilingSdkCustomizer(registry);

  @Nested
  class SnapshotProfilingDisabledByDefaultTest {
    @RegisterExtension
    public final OpenTelemetrySdkExtension s =
        OpenTelemetrySdkExtension.builder().with(customizer).build();

    @ParameterizedTest
    @SpanKinds.Entry
    void snapshotProfilingIsDisabledByDefault(SpanKind kind, Tracer tracer) {
      var root = tracer.spanBuilder("root").setSpanKind(kind).startSpan();
      assertFalse(registry.isRegistered(root.getSpanContext()));
    }
  }

  @Nested
  class SnapshotProfilingEnabledTest {
    @RegisterExtension
    public final OpenTelemetrySdkExtension s =
        OpenTelemetrySdkExtension.builder()
            .with(customizer)
            .withProperty("splunk.snapshot.profiler.enabled", "true")
            .build();

    @ParameterizedTest
    @SpanKinds.Entry
    void snapshotProfilingIsExplicitlyEnabled(SpanKind kind, Tracer tracer) {
      var root = tracer.spanBuilder("root").setSpanKind(kind).startSpan();
      assertTrue(registry.isRegistered(root.getSpanContext()));
    }
  }

  @Nested
  class SnapshotProfilingDisabledTest {
    @RegisterExtension
    public final OpenTelemetrySdkExtension s =
        OpenTelemetrySdkExtension.builder()
            .with(customizer)
            .withProperty("splunk.snapshot.profiler.enabled", "false")
            .build();

    @ParameterizedTest
    @SpanKinds.Entry
    void snapshotProfilingIsExplicitlyEnabled(SpanKind kind, Tracer tracer) {
      var root = tracer.spanBuilder("root").setSpanKind(kind).startSpan();
      assertFalse(registry.isRegistered(root.getSpanContext()));
    }
  }
}
