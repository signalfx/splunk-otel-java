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

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.trace.IdGenerator;
import org.junit.jupiter.api.Test;

class TraceRegistryTest {
  private final IdGenerator idGenerator = IdGenerator.random();
  private final TraceRegistry registry = new TraceRegistry();

  @Test
  void registerTrace() {
    var spanContext = newSpanContext();
    registry.register(spanContext);
    assertTrue(registry.isRegistered(spanContext));
  }

  @Test
  void unregisteredTracesAreNotRegisteredForProfiling() {
    var spanContext = newSpanContext();
    assertFalse(registry.isRegistered(spanContext));
  }

  @Test
  void unregisterTraceForProfiling() {
    var spanContext = newSpanContext();

    registry.register(spanContext);
    registry.unregister(spanContext);

    assertFalse(registry.isRegistered(spanContext));
  }

  private SpanContext newSpanContext() {
    return newSpanContext(randomTraceId());
  }

  private SpanContext newSpanContext(String traceId) {
    var spanId = idGenerator.generateSpanId();
    return SpanContext.create(traceId, spanId, TraceFlags.getDefault(), TraceState.getDefault());
  }

  private String randomTraceId() {
    return idGenerator.generateTraceId();
  }
}
