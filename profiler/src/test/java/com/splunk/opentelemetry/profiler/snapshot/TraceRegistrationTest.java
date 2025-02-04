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
import io.opentelemetry.context.Context;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;

class TraceRegistrationTest {
  private final TraceRegistry registry = new TraceRegistry();
  private final SnapshotProfilingSdkCustomizer customizer =
      new SnapshotProfilingSdkCustomizer(registry);

  @RegisterExtension
  public final OpenTelemetrySdkExtension s =
      OpenTelemetrySdkExtension.builder()
          .withProperty("splunk.snapshot.profiler.enabled", "true")
          .with(customizer)
          .build();

  @ParameterizedTest
  @SpanKinds.Entry
  void registerTraceForProfilingWhenRootSpanStarts(SpanKind kind, Tracer tracer) {
    var root = tracer.spanBuilder("root").setSpanKind(kind).startSpan();
    assertTrue(registry.isRegistered(root.getSpanContext()));
  }

  @ParameterizedTest
  @SpanKinds.NonEntry
  void onlyRegisterTraceForProfilingWhenRootSpanIsEntrySpan(SpanKind kind, Tracer tracer) {
    var root = tracer.spanBuilder("root").setSpanKind(kind).startSpan();
    assertFalse(registry.isRegistered(root.getSpanContext()));
  }

  @ParameterizedTest
  @SpanKinds.Entry
  void unregisterTraceForProfilingWhenEntrySpanEnds(SpanKind kind, Tracer tracer) {
    var root = tracer.spanBuilder("root").setSpanKind(kind).startSpan();
    root.end();
    assertFalse(registry.isRegistered(root.getSpanContext()));
  }

  @ParameterizedTest
  @SpanKinds.Entry
  void doNotRegisterTraceForProfilingWhenNonRootSpanDetected(SpanKind kind, Tracer tracer) {
    var root = tracer.spanBuilder("root").setSpanKind(kind).startSpan();
    root.end();
    var child =
        tracer
            .spanBuilder("child")
            .setSpanKind(kind)
            .setParent(Context.current().with(root))
            .startSpan();
    assertFalse(registry.isRegistered(child.getSpanContext()));
  }

  @ParameterizedTest
  @SpanKinds.NonEntry
  void onlyUnregisterTraceForProfilingWhenEntrySpanEnds(SpanKind kind, Tracer tracer) {
    var root = tracer.spanBuilder("root").setSpanKind(SpanKind.SERVER).startSpan();
    var child =
        tracer
            .spanBuilder("child")
            .setSpanKind(kind)
            .setParent(Context.current().with(root))
            .startSpan();
    child.end();
    assertTrue(registry.isRegistered(root.getSpanContext()));
  }
}
