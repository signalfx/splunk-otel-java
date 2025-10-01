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
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkExtension;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SnapshotSpanAttributeTest {
  private final TogglableTraceRegistry registry = new TogglableTraceRegistry();
  private final SnapshotProfilingSdkCustomizer customizer =
      Snapshotting.customizer().with(registry).build();

  @RegisterExtension
  public final OpenTelemetrySdkExtension s =
      OpenTelemetrySdkExtension.configure()
          .with(customizer)
          .withProperty("splunk.snapshot.profiler.enabled", "true")
          .build();

  @Test
  void addSnapshotSpanAttributeToEntrySpans(Tracer tracer) {
    try (var ignored = Context.root().with(Volume.HIGHEST).makeCurrent()) {
      var span = (ReadWriteSpan) tracer.spanBuilder("root").startSpan();
      var attribute = span.getAttribute(AttributeKey.booleanKey("splunk.snapshot.profiling"));
      assertThat(attribute).isTrue();
    }
  }

  @Test
  void doNotAddSnapshotSpanAttributeToNonEntrySpans(Tracer tracer) {
    try (var ignored1 = Context.root().with(Volume.HIGHEST).makeCurrent()) {
      var root = tracer.spanBuilder("root").startSpan();
      try (var ignored2 = root.makeCurrent()) {
        var client = (ReadWriteSpan) tracer.spanBuilder("client").startSpan();
        var attribute = client.getAttribute(AttributeKey.booleanKey("splunk.snapshot.profiling"));
        assertThat(attribute).isNull();
      }
    }
  }

  @Test
  void addSnapshotSpanAttributeToAllEntrySpans(Tracer tracer) {
    try (var ignored1 = Context.root().with(Volume.HIGHEST).makeCurrent()) {
      try (var ignored2 = tracer.spanBuilder("root").startSpan().makeCurrent()) {
        var client = tracer.spanBuilder("client").startSpan();
        try (var ignored3 = client.makeCurrent()) {

          var remoteSpanContext = Snapshotting.spanContext().remoteFrom(client).build();
          var remoteParentSpan = Span.wrap(remoteSpanContext);
          try (var ignored4 = Context.current().with(remoteParentSpan).makeCurrent()) {
            var downstreamEntry = (ReadWriteSpan) tracer.spanBuilder("downstreamEntry").startSpan();
            var attribute =
                downstreamEntry.getAttribute(AttributeKey.booleanKey("splunk.snapshot.profiling"));
            assertThat(attribute).isTrue();
          }
        }
      }
    }
  }

  @Test
  void doNotAddSnapshotSpanAttributeWhenTraceIsNotRegisteredForSnapshotting(Tracer tracer) {
    registry.toggle(State.OFF);

    try (var ignored = Context.root().with(Volume.HIGHEST).makeCurrent()) {
      var span = (ReadWriteSpan) tracer.spanBuilder("root").startSpan();
      var attribute = span.getAttribute(AttributeKey.booleanKey("splunk.snapshot.profiling"));
      assertThat(attribute).isNull();
    }
  }
}
