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

import static com.splunk.opentelemetry.instrumentation.jvmmetrics.GcMemoryMetrics.METRIC_NAME;

import com.splunk.opentelemetry.instrumentation.jvmmetrics.GcMemoryMetrics;
import io.opentelemetry.api.metrics.Meter;

public class OtelGcMemoryMetrics {

  public void install() {
    new GcMemoryMetrics()
        .install(
            deltaSum -> {
              Meter meter = OtelMeterProvider.get();
              meter
                  .counterBuilder(METRIC_NAME)
                  .setUnit("bytes")
                  .setDescription("Sum of heap size differences before and after gc.")
                  .buildWithCallback(measurement -> measurement.record(deltaSum.get()));
            });
  }
}
