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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.Collections;

class TraceIdBasedSnapshotSelector implements SnapshotSelector {
  private final Sampler sampler;

  TraceIdBasedSnapshotSelector(double selectionProbability) {
    if (selectionProbability < 0 || selectionProbability > 1) {
      throw new IllegalArgumentException("Selection probability must be between 0 and 1.");
    }
    this.sampler = Sampler.traceIdRatioBased(selectionProbability);
  }

  @Override
  public boolean select(Context context) {
    SpanContext spanContext = Span.fromContext(context).getSpanContext();
    if (!spanContext.isValid()) {
      return false;
    }

    return shouldSample(spanContext.getTraceId()) == SamplingDecision.RECORD_AND_SAMPLE;
  }

  /**
   * Trace ID ratio sampling only considers trace ID so other parameters can safely be placeholders.
   */
  private SamplingDecision shouldSample(String traceId) {
    SamplingResult result =
        sampler.shouldSample(
            Context.root(),
            traceId,
            "",
            SpanKind.INTERNAL,
            Attributes.empty(),
            Collections.emptyList());
    return result.getDecision();
  }
}
