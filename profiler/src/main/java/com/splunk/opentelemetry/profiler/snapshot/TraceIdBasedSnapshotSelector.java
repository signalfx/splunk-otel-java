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

import io.opentelemetry.api.trace.SpanContext;

class TraceIdBasedSnapshotSelector implements SnapshotSelector {
  // the length of trace-id represented as a hex string
  private static final int TRACE_ID_LENGTH = 32;
  // number of characters taken from the tail of trace-id as the trace randomness
  private static final int CHARS = 7;
  private static final String HEX_FORMATTER = "%07x";
  // to convert probability to threshold
  private static final int MULTIPLIER = (1 << (4 * CHARS)) - 1;
  private final String threshold;

  TraceIdBasedSnapshotSelector(double selectionProbability) {
    if (selectionProbability < 0 || selectionProbability > 1) {
      throw new IllegalArgumentException("Selection probability must be between 0 and 1.");
    }

    this.threshold = thresholdFor(selectionProbability);
  }

  private static String thresholdFor(double probability) {
    int threshold = (int) (probability * MULTIPLIER);
    if (threshold == 0) {
      // Zero or near-zero probability, special case
      return null;
    }
    return String.format(HEX_FORMATTER, threshold);
  }

  @Override
  public boolean select(SpanContext spanContext) {
    if (threshold == null || !spanContext.isValid()) {
      return false;
    }

    String traceId = spanContext.getTraceId();
    String randomness = traceId.substring(TRACE_ID_LENGTH - CHARS);
    // Select if randomness is lesser or equal to threshold
    return randomness.compareTo(threshold) < 1;
  }
}
