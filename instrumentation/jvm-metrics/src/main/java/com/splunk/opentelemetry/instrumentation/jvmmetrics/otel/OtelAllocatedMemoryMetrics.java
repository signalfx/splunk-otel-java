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

package com.splunk.opentelemetry.instrumentation.jvmmetrics.otel;

import static com.splunk.opentelemetry.instrumentation.jvmmetrics.AllocatedMemoryMetrics.METRIC_NAME;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import com.splunk.opentelemetry.instrumentation.jvmmetrics.AllocatedMemoryMetrics;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;

public class OtelAllocatedMemoryMetrics {
  private static final AttributeKey<String> TYPE = stringKey("type");

  public void install() {
    AllocatedMemoryMetrics allocatedMemoryMetrics = new AllocatedMemoryMetrics();
    if (!allocatedMemoryMetrics.isAvailable()) {
      return;
    }

    Meter meter = OtelMeterProvider.get();
    Attributes attributes = Attributes.of(TYPE, "heap");
    meter
        .counterBuilder(METRIC_NAME)
        .setUnit("bytes")
        .setDescription("Approximate sum of heap allocations.")
        .buildWithCallback(
            measurement ->
                measurement.record(
                    allocatedMemoryMetrics.getCumulativeAllocationTotal(), attributes));
  }
}
