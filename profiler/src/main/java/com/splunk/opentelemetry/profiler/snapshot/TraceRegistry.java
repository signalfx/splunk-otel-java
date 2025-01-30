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

interface TraceRegistry {
  void register(SpanContext spanContext);

  boolean isRegistered(SpanContext spanContext);

  void unregister(SpanContext spanContext);
}
