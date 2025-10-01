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

import com.splunk.opentelemetry.profiler.snapshot.registry.TraceRegistry;
import com.splunk.opentelemetry.profiler.snapshot.registry.TraceRegistryHolder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class TraceRegistrationTest {
  private final TraceRegistry registry = TraceRegistryHolder.getTraceRegistry();
  private final SnapshotProfilingSdkCustomizer customizer =
      Snapshotting.customizer().with(registry).build();

  @RegisterExtension
  public final OpenTelemetrySdkExtension s =
      OpenTelemetrySdkExtension.configure()
          .withProperty("splunk.snapshot.profiler.enabled", "true")
          .with(customizer)
          .build();

  @Test
  void registerTraceForProfilingWhenRootSpanStarts(Tracer tracer) {
    try (var ignored = Context.current().with(Volume.HIGHEST).makeCurrent()) {
      var root = tracer.spanBuilder("root").startSpan();
      assertThat(registry.isRegistered(root.getSpanContext())).isTrue();
    }
  }

  @Test
  void registerTraceForProfilingWhenDownstreamEntryStarts(Tracer tracer) {
    var remoteSpanContext = Snapshotting.spanContext().remote().build();
    var remoteParent = Span.wrap(remoteSpanContext);

    try (var ignored = Context.current().with(Volume.HIGHEST).with(remoteParent).makeCurrent()) {
      var root = tracer.spanBuilder("root").startSpan();
      assertThat(registry.isRegistered(root.getSpanContext())).isTrue();
    }
  }

  @Test
  void unregisterTraceForProfilingWhenEntrySpanEnds(Tracer tracer) {
    try (var ignored = Context.current().with(Volume.HIGHEST).makeCurrent()) {
      var root = tracer.spanBuilder("root").startSpan();
      root.end();
      assertThat(registry.isRegistered(root.getSpanContext())).isFalse();
    }
  }

  @Test
  void doNotRegisterTraceForProfilingWhenSnapshotVolumeIsOff(Tracer tracer) {
    try (var ignored = Context.current().with(Volume.OFF).makeCurrent()) {
      var root = tracer.spanBuilder("root").startSpan();
      assertThat(registry.isRegistered(root.getSpanContext())).isFalse();
    }
  }

  @Test
  void doNotRegisterTraceForProfilingWhenSnapshotVolumeIsNotfound(Tracer tracer) {
    var root = tracer.spanBuilder("root").startSpan();
    assertThat(registry.isRegistered(root.getSpanContext())).isFalse();
  }

  @Test
  void onlyUnregisterTraceForProfilingWhenEntrySpanEnds(Tracer tracer) {
    try (var ignored = Context.current().with(Volume.HIGHEST).makeCurrent()) {
      var root = tracer.spanBuilder("root").startSpan();
      var child = tracer.spanBuilder("child").setParent(Context.current().with(root)).startSpan();
      child.end();
      assertThat(registry.isRegistered(root.getSpanContext())).isTrue();
    }
  }
}
