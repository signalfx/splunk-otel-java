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

package com.splunk.opentelemetry.instrumentation.liberty.metrics;

import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.jmx.JmxAttributesHelper.getNumberAttribute;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ThreadPoolSemanticConventions.EXECUTOR_NAME;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ThreadPoolSemanticConventions.EXECUTOR_TYPE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ThreadPoolSemanticConventions.THREADS_ACTIVE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ThreadPoolSemanticConventions.THREADS_CURRENT;
import static java.util.Arrays.asList;

import com.splunk.opentelemetry.javaagent.bootstrap.jmx.JmxQuery;
import com.splunk.opentelemetry.javaagent.bootstrap.metrics.jmx.JmxMetricsWatcher;
import com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.jmx.MicrometerJmxMetricsWatcherFactory;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.MBeanServer;
import javax.management.ObjectName;

final class LibertyThreadPoolMicrometerMetrics {

  private static final AtomicBoolean initialized = new AtomicBoolean();
  private static final JmxMetricsWatcher<Meter> jmxMetricsWatcher =
      MicrometerJmxMetricsWatcherFactory.create(
          JmxQuery.create("WebSphere", "type", "ThreadPoolStats"),
          LibertyThreadPoolMicrometerMetrics::createMeters);

  public static void initialize() {
    if (initialized.compareAndSet(false, true)) {
      jmxMetricsWatcher.start();
    }
  }

  private static List<Meter> createMeters(MBeanServer mBeanServer, ObjectName objectName) {
    Tags tags = executorTags(objectName);
    return asList(
        THREADS_CURRENT.create(tags, mBeanServer, getNumberAttribute(objectName, "PoolSize")),
        THREADS_ACTIVE.create(tags, mBeanServer, getNumberAttribute(objectName, "ActiveThreads")));
  }

  private static Tags executorTags(ObjectName objectName) {
    // use the "name" property if available
    String name = objectName.getKeyProperty("name");
    // if its unavailable just use the whole mbean name
    if (name == null) {
      name = objectName.toString();
    }
    return Tags.of(Tag.of(EXECUTOR_TYPE, "liberty"), Tag.of(EXECUTOR_NAME, name));
  }

  private LibertyThreadPoolMicrometerMetrics() {}
}
