/*
 * 2024 Copyright (C) AppDynamics, Inc., and its affiliates
 * All Rights Reserved
 */

/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
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
  private final SnapshotProfilingSdkCustomizer customizer = new SnapshotProfilingSdkCustomizer(registry);

  @RegisterExtension
  public final OpenTelemetrySdkExtension s = OpenTelemetrySdkExtension.builder()
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
    var child = tracer.spanBuilder("child").setSpanKind(kind).setParent(Context.current().with(root)).startSpan();
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
