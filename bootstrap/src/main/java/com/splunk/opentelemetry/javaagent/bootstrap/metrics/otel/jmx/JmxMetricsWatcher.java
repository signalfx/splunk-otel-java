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

package com.splunk.opentelemetry.javaagent.bootstrap.metrics.otel.jmx;

import com.splunk.opentelemetry.javaagent.bootstrap.jmx.JmxQuery;
import com.splunk.opentelemetry.javaagent.bootstrap.jmx.JmxWatcher;
import com.splunk.opentelemetry.javaagent.bootstrap.metrics.jmx.AbstractJmxMetricsWatcher;

public final class JmxMetricsWatcher extends AbstractJmxMetricsWatcher<AutoCloseable> {

  public JmxMetricsWatcher(JmxQuery query, OtelMetersFactory metersFactory) {
    super(query, metersFactory);
  }

  // visible for tests
  // this constructor has a trailing unused argument to make sure @InjectMocks chooses it over
  // the other constructor
  JmxMetricsWatcher(JmxWatcher jmxWatcher, OtelMetersFactory metersFactory, Object unused) {
    super(jmxWatcher, metersFactory);
  }

  @Override
  protected void unregister(AutoCloseable instrument) {
    try {
      instrument.close();
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to close instrument", exception);
    }
  }
}
