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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;

/**
 * This sampler drops span of INTERNAL kind without parent. The goal is to silence traces with
 * INTERNAL root spans, because in most cases those traces are created by scheduled jobs and are of
 * no interest.
 *
 * <p>NB! Currently this sampler will drop even spans/traces created by known scheduled
 * instrumentations such as Spring Scheduling. It cannot be done until OpenTelemetry spec is
 * updated, see https://github.com/open-telemetry/opentelemetry-specification/issues/1588. For the
 * same reason this sampler will drop INTERNAL spans created by user manual instrumentations.
 */
public class InternalRootOffSampler implements Sampler {

  public static final SamplingResult RECORD =
      SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE);
  public static final SamplingResult DROP = SamplingResult.create(SamplingDecision.DROP);

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {
    SpanContext parentSpanContext = Span.fromContext(parentContext).getSpanContext();
    if (!parentSpanContext.isValid()) {
      if (spanKind == SpanKind.SERVER || spanKind == SpanKind.CONSUMER) {
        return RECORD;
      } else {
        return DROP;
      }
    } else {
      return parentSpanContext.isSampled() ? RECORD : DROP;
    }
  }

  @Override
  public String getDescription() {
    return "InternalRootOffSampler";
  }
}
