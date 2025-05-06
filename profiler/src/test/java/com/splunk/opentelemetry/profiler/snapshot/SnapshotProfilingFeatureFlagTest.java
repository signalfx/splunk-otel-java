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

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SnapshotProfilingFeatureFlagTest {
  private final ITraceRegistry registry = new TraceRegistry();
  private final SnapshotProfilingSdkCustomizer customizer =
      Snapshotting.customizer().with(registry).build();

  @Nested
  class SnapshotProfilingDisabledByDefaultTest {
    @RegisterExtension
    public final OpenTelemetrySdkExtension s =
        OpenTelemetrySdkExtension.configure().with(customizer).build();

    @Test
    void snapshotProfilingIsDisabledByDefault(Tracer tracer) {
      try (var ignored = Context.current().with(Volume.HIGHEST).makeCurrent()) {
        var root = tracer.spanBuilder("root").startSpan();
        assertThat(registry.isRegistered(root.getSpanContext())).isFalse();
      }
    }
  }

  @Nested
  class SnapshotProfilingEnabledTest {
    @RegisterExtension
    public final OpenTelemetrySdkExtension s =
        OpenTelemetrySdkExtension.configure()
            .with(customizer)
            .withProperty("splunk.snapshot.profiler.enabled", "true")
            .build();

    @Test
    void snapshotProfilingIsExplicitlyEnabled(Tracer tracer) {
      try (var ignored = Context.current().with(Volume.HIGHEST).makeCurrent()) {
        var root = tracer.spanBuilder("root").startSpan();
        assertThat(registry.isRegistered(root.getSpanContext())).isTrue();
      }
    }
  }

  @Nested
  class SnapshotProfilingDisabledTest {
    @RegisterExtension
    public final OpenTelemetrySdkExtension s =
        OpenTelemetrySdkExtension.configure()
            .with(customizer)
            .withProperty("splunk.snapshot.profiler.enabled", "false")
            .build();

    @Test
    void snapshotProfilingIsExplicitlyEnabled(Tracer tracer) {
      try (var ignored = Context.current().with(Volume.HIGHEST).makeCurrent()) {
        var root = tracer.spanBuilder("root").startSpan();
        assertThat(registry.isRegistered(root.getSpanContext())).isFalse();
      }
    }
  }
}
