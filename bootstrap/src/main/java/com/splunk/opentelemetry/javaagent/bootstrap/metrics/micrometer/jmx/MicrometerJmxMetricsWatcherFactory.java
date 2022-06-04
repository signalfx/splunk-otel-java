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

package com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.jmx;

import com.splunk.opentelemetry.javaagent.bootstrap.jmx.JmxQuery;
import com.splunk.opentelemetry.javaagent.bootstrap.jmx.JmxWatcher;
import com.splunk.opentelemetry.javaagent.bootstrap.metrics.jmx.JmxMetricsWatcher;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;

public final class MicrometerJmxMetricsWatcherFactory {

  public static JmxMetricsWatcher<Meter> create(
      JmxQuery query, MicrometerMetersFactory metersFactory) {
    return new JmxMetricsWatcher<>(
        query, metersFactory, (meter) -> Metrics.globalRegistry.remove(meter));
  }

  // visible for tests
  static JmxMetricsWatcher<Meter> create(
      JmxWatcher jmxWatcher, MeterRegistry meterRegistry, MicrometerMetersFactory metersFactory) {
    return new JmxMetricsWatcher<>(
        jmxWatcher, metersFactory, (meter) -> meterRegistry.remove(meter));
  }
}
