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

package com.splunk.opentelemetry;

import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import java.util.Collection;
import java.util.Objects;

class MetricsInspector {
  private final Collection<ExportMetricsServiceRequest> requests;

  MetricsInspector(Collection<ExportMetricsServiceRequest> requests) {
    this.requests = requests;
  }

  boolean hasMetricsNamed(String metricName) {
    return requests.stream()
        .flatMap(list -> list.getResourceMetricsList().stream())
        .flatMap(list -> list.getInstrumentationLibraryMetricsList().stream())
        .flatMap(list -> list.getMetricsList().stream())
        .anyMatch(metric -> Objects.equals(metric.getName(), metricName));
  }
}
