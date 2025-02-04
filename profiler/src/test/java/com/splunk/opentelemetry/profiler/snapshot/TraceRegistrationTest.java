/*
 * 2024 Copyright (C) AppDynamics, Inc., and its affiliates
 * All Rights Reserved
 */

/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceRegistrationTest {
  private final DefaultTraceRegistry registry = new DefaultTraceRegistry();
  private final SnapshotProfilingSdkCustomizer customizer = new SnapshotProfilingSdkCustomizer(registry);

  @RegisterExtension
  public final OpenTelemetrySdkExtension s = OpenTelemetrySdkExtension.builder()
          .withProperty("splunk.snapshot.profiler.enabled", "true")
          .with(customizer)
          .build();

  @ParameterizedTest
  @EnumSource(
      value = SpanKind.class,
      mode = Mode.INCLUDE,
      names = {"SERVER", "CONSUMER"})
  void registerTraceForProfilingWhenRootSpanStarts(SpanKind kind, Tracer tracer) {
    var root = tracer.spanBuilder("root").setSpanKind(kind).startSpan();
    assertTrue(registry.isRegistered(root.getSpanContext()));
  }

  @ParameterizedTest
  @EnumSource(
      value = SpanKind.class,
      mode = Mode.EXCLUDE,
      names = {"SERVER", "CONSUMER"})
  void onlyRegisterTraceForProfilingWhenRootSpanIsEntrySpan(SpanKind kind, Tracer tracer) {
    var root = tracer.spanBuilder("root").setSpanKind(kind).startSpan();
    assertFalse(registry.isRegistered(root.getSpanContext()));
  }

  @ParameterizedTest
  @EnumSource(
      value = SpanKind.class,
      mode = Mode.INCLUDE,
      names = {"SERVER", "CONSUMER"})
  void unregisterTraceForProfilingWhenEntrySpanEnds(SpanKind kind, Tracer tracer) {
    var root = tracer.spanBuilder("root").setSpanKind(kind).startSpan();
    root.end();
    assertFalse(registry.isRegistered(root.getSpanContext()));
  }

  @ParameterizedTest
  @EnumSource(
      value = SpanKind.class,
      mode = Mode.INCLUDE,
      names = {"SERVER", "CONSUMER"})
  void doNotRegisterTraceForProfilingWhenNonRootSpanDetected(SpanKind kind, Tracer tracer) {
    var root = tracer.spanBuilder("root").setSpanKind(kind).startSpan();
    root.end();
    var child = tracer.spanBuilder("child").setSpanKind(kind).setParent(Context.current().with(root)).startSpan();
    assertFalse(registry.isRegistered(child.getSpanContext()));
  }

  @ParameterizedTest
  @EnumSource(
      value = SpanKind.class,
      mode = Mode.EXCLUDE,
      names = {"SERVER", "CONSUMER"})
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
