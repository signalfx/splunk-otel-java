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

package com.splunk.opentelemetry.instrumentation.weblogic.metrics;

import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.jmx.JmxAttributesHelper.getNumberAttribute;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ThreadPoolSemanticConventions.EXECUTOR_NAME;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ThreadPoolSemanticConventions.EXECUTOR_TYPE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ThreadPoolSemanticConventions.TASKS_COMPLETED;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ThreadPoolSemanticConventions.THREADS_CURRENT;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ThreadPoolSemanticConventions.THREADS_IDLE;
import static java.util.Arrays.asList;

import com.splunk.opentelemetry.javaagent.bootstrap.jmx.JmxQuery;
import com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.jmx.JmxMetricsWatcher;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.MBeanServer;
import javax.management.ObjectName;

final class WebLogicThreadPoolMicrometerMetrics {

  private static final AtomicBoolean initialized = new AtomicBoolean();
  private static final JmxMetricsWatcher jmxMetricsWatcher =
      new JmxMetricsWatcher(
          JmxQuery.create("com.bea", "Name", "ThreadPoolRuntime"),
          WebLogicThreadPoolMicrometerMetrics::createMeters);

  public static void initialize() {
    if (initialized.compareAndSet(false, true)) {
      jmxMetricsWatcher.start();
    }
  }

  private static List<Meter> createMeters(MBeanServer mBeanServer, ObjectName objectName) {
    Tags tags = executorTags(objectName);
    return asList(
        THREADS_CURRENT.create(
            tags, mBeanServer, getNumberAttribute(objectName, "ExecuteThreadTotalCount")),
        TASKS_COMPLETED.create(
            tags, mBeanServer, getNumberAttribute(objectName, "CompletedRequestCount")),
        // WebLogic puts threads that are not needed to handle the present work load into a standby
        // pool. Include these in idle thread count.
        THREADS_IDLE.create(
            tags,
            () ->
                getNumberAttribute(objectName, "ExecuteThreadIdleCount").applyAsDouble(mBeanServer)
                    + getNumberAttribute(objectName, "StandbyThreadCount")
                        .applyAsDouble(mBeanServer)));
  }

  private static Tags executorTags(ObjectName objectName) {
    return Tags.of(Tag.of(EXECUTOR_TYPE, "weblogic"), Tag.of(EXECUTOR_NAME, objectName.toString()));
  }

  private WebLogicThreadPoolMicrometerMetrics() {}
}
