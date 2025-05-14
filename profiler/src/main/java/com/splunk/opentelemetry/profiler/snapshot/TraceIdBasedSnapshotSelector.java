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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;

class TraceIdBasedSnapshotSelector implements SnapshotSelector {
  private final int percentile;

  TraceIdBasedSnapshotSelector(double selectionRate) {
    if (selectionRate < 0 || selectionRate > 1) {
      throw new IllegalArgumentException("Selection rate must be between 0 and 1.");
    }
    this.percentile = (int) (selectionRate * 100);
  }

  @Override
  public boolean select(Context context) {
    SpanContext spanContext = Span.fromContext(context).getSpanContext();
    if (!spanContext.isValid()) {
      return false;
    }

    int hash = Math.abs(spanContext.getTraceId().hashCode());
    int tracePercentile = hash % 100;
    return tracePercentile <= percentile;
  }
}
