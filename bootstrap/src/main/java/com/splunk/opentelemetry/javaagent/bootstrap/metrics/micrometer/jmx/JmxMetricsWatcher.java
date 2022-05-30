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
import com.splunk.opentelemetry.javaagent.bootstrap.metrics.jmx.AbstractJmxMetricsWatcher;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;

public final class JmxMetricsWatcher extends AbstractJmxMetricsWatcher<Meter> {

  private final MeterRegistry meterRegistry;

  public JmxMetricsWatcher(JmxQuery query, MicrometerMetersFactory metersFactory) {
    super(query, metersFactory);
    this.meterRegistry = Metrics.globalRegistry;
  }

  // visible for tests
  JmxMetricsWatcher(
      JmxWatcher jmxWatcher, MeterRegistry meterRegistry, MicrometerMetersFactory metersFactory) {
    super(jmxWatcher, metersFactory);
    this.meterRegistry = meterRegistry;
  }

  @Override
  protected void unregister(Meter meter) {
    meterRegistry.remove(meter);
  }
}
