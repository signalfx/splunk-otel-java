/*
 * 2024 Copyright (C) AppDynamics, Inc., and its affiliates
 * All Rights Reserved
 */

/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.splunk.opentelemetry.profiler.snapshot;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

public class SnapshotProfilingSpanProcessor implements SpanProcessor {
  private final TraceRegistry registry;

  SnapshotProfilingSpanProcessor(TraceRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void onStart(Context context, ReadWriteSpan span) {
    if (isRoot(span) && isEntry(span)) {
      registry.register(span.getSpanContext());
    }
  }

  private boolean isRoot(ReadableSpan span) {
    return SpanContext.getInvalid().equals(span.getParentSpanContext());
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {
    if (isEntry(span)) {
      registry.unregister(span.getSpanContext());
    }
  }

  private boolean isEntry(ReadableSpan span) {
    return span.getKind() == SpanKind.SERVER || span.getKind() == SpanKind.CONSUMER;
  }

  @Override
  public boolean isEndRequired() {
    return true;
  }
}
