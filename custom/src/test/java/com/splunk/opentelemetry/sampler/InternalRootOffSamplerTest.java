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

package com.splunk.opentelemetry.sampler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class InternalRootOffSamplerTest {
  private final IdGenerator idsGenerator = IdGenerator.random();
  private final String traceId = idsGenerator.generateTraceId();
  private final String parentSpanId = idsGenerator.generateSpanId();
  private final SpanContext sampledSpanContext =
      SpanContext.create(traceId, parentSpanId, TraceFlags.getSampled(), TraceState.getDefault());
  private final Context sampledParentContext = Context.root().with(Span.wrap(sampledSpanContext));
  private final Context notSampledParentContext =
      Context.root()
          .with(
              Span.wrap(
                  SpanContext.create(
                      traceId, parentSpanId, TraceFlags.getDefault(), TraceState.getDefault())));
  private final Context invalidContext = Context.root().with(Span.getInvalid());

  @Test
  void shouldHonourParentDecision() {
    InternalRootOffSampler sampler = new InternalRootOffSampler();

    String traceId = idsGenerator.generateTraceId();
    String spanId = idsGenerator.generateSpanId();
    assertEquals(
        SamplingDecision.RECORD_AND_SAMPLE,
        sampler
            .shouldSample(
                sampledParentContext,
                traceId,
                spanId,
                SpanKind.INTERNAL,
                Attributes.empty(),
                Collections.emptyList())
            .getDecision());
    assertEquals(
        SamplingDecision.DROP,
        sampler
            .shouldSample(
                notSampledParentContext,
                traceId,
                spanId,
                SpanKind.INTERNAL,
                Attributes.empty(),
                Collections.emptyList())
            .getDecision());
  }

  @Test
  void shouldSampleServerRoot() {
    InternalRootOffSampler sampler = new InternalRootOffSampler();

    String traceId = idsGenerator.generateTraceId();
    String spanId = idsGenerator.generateSpanId();
    assertEquals(
        SamplingDecision.RECORD_AND_SAMPLE,
        sampler
            .shouldSample(
                invalidContext,
                traceId,
                spanId,
                SpanKind.SERVER,
                Attributes.empty(),
                Collections.emptyList())
            .getDecision());
  }

  @Test
  void shouldSampleConsumerRoot() {
    InternalRootOffSampler sampler = new InternalRootOffSampler();

    String traceId = idsGenerator.generateTraceId();
    String spanId = idsGenerator.generateSpanId();
    assertEquals(
        SamplingDecision.RECORD_AND_SAMPLE,
        sampler
            .shouldSample(
                invalidContext,
                traceId,
                spanId,
                SpanKind.CONSUMER,
                Attributes.empty(),
                Collections.emptyList())
            .getDecision());
  }

  @Test
  void shouldDropInternalRoot() {
    InternalRootOffSampler sampler = new InternalRootOffSampler();

    String traceId = idsGenerator.generateTraceId();
    String spanId = idsGenerator.generateSpanId();
    assertEquals(
        SamplingDecision.DROP,
        sampler
            .shouldSample(
                invalidContext,
                traceId,
                spanId,
                SpanKind.INTERNAL,
                Attributes.empty(),
                Collections.emptyList())
            .getDecision());
  }

  @Test
  void shouldDropClientRoot() {
    InternalRootOffSampler sampler = new InternalRootOffSampler();

    String traceId = idsGenerator.generateTraceId();
    String spanId = idsGenerator.generateSpanId();
    assertEquals(
        SamplingDecision.DROP,
        sampler
            .shouldSample(
                invalidContext,
                traceId,
                spanId,
                SpanKind.CLIENT,
                Attributes.empty(),
                Collections.emptyList())
            .getDecision());
  }

  @Test
  void shouldDropProducerRoot() {
    InternalRootOffSampler sampler = new InternalRootOffSampler();

    String traceId = idsGenerator.generateTraceId();
    String spanId = idsGenerator.generateSpanId();
    assertEquals(
        SamplingDecision.DROP,
        sampler
            .shouldSample(
                invalidContext,
                traceId,
                spanId,
                SpanKind.PRODUCER,
                Attributes.empty(),
                Collections.emptyList())
            .getDecision());
  }
}
