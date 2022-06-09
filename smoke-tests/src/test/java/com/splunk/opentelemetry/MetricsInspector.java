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
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class MetricsInspector {
  private final Collection<ExportMetricsServiceRequest> requests;

  MetricsInspector(Collection<ExportMetricsServiceRequest> requests) {
    this.requests = requests;
  }

  boolean hasMetricsNamed(String metricName) {
    return requests.stream()
        .flatMap(list -> list.getResourceMetricsList().stream())
        .flatMap(list -> list.getScopeMetricsList().stream())
        .flatMap(list -> list.getMetricsList().stream())
        .anyMatch(metric -> Objects.equals(metric.getName(), metricName));
  }

  boolean hasSumWithAttributes(String metricName, Map<String, String> attributes) {
    return requests.stream()
        .flatMap(list -> list.getResourceMetricsList().stream())
        .flatMap(list -> list.getScopeMetricsList().stream())
        .flatMap(list -> list.getMetricsList().stream())
        .filter(metric -> Objects.equals(metric.getName(), metricName))
        .flatMap(metric -> metric.getSum().getDataPointsList().stream())
        .anyMatch(dataPoint -> hasAttributes(dataPoint, attributes));
  }

  boolean hasGaugeWithAttributes(String name, Map<String, String> attributes) {
    return requests.stream()
        .flatMap(list -> list.getResourceMetricsList().stream())
        .flatMap(list -> list.getScopeMetricsList().stream())
        .flatMap(list -> list.getMetricsList().stream())
        .filter(Metric::hasGauge)
        .filter(metric -> Objects.equals(metric.getName(), name))
        .flatMap(metric -> metric.getGauge().getDataPointsList().stream())
        .anyMatch(dataPoint -> hasAttributes(dataPoint, attributes));
  }

  private boolean hasAttributes(NumberDataPoint dataPoint, Map<String, String> attributes) {
    // make it mutable
    attributes = new HashMap<>(attributes);
    for (KeyValue kv : dataPoint.getAttributesList()) {
      String value = attributes.get(kv.getKey());
      if (Objects.equals(value, kv.getValue().getStringValue())) {
        attributes.remove(kv.getKey());
      }
      if (attributes.isEmpty()) {
        return true;
      }
    }
    return false;
  }
}
