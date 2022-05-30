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

import com.splunk.opentelemetry.javaagent.bootstrap.jmx.JmxQuery;
import com.splunk.opentelemetry.javaagent.bootstrap.metrics.otel.ThreadPoolMetrics;
import com.splunk.opentelemetry.javaagent.bootstrap.metrics.otel.jmx.JmxMetricsWatcher;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.MBeanServer;
import javax.management.ObjectName;

final class WebLogicThreadPoolOtelMetrics {
  private static final String INSTRUMENTATION_NAME =
      "com.splunk.javaagent.weblogic-thread-pool-metrics";

  private static final AtomicBoolean initialized = new AtomicBoolean();
  private static final JmxMetricsWatcher jmxMetricsWatcher =
      new JmxMetricsWatcher(
          JmxQuery.create("com.bea", "Name", "ThreadPoolRuntime"),
          WebLogicThreadPoolOtelMetrics::createMeters);

  public static void initialize() {
    if (initialized.compareAndSet(false, true)) {
      jmxMetricsWatcher.start();
    }
  }

  private static List<AutoCloseable> createMeters(MBeanServer mBeanServer, ObjectName objectName) {
    OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
    ThreadPoolMetrics metrics =
        ThreadPoolMetrics.create(
            openTelemetry, INSTRUMENTATION_NAME, "weblogic", objectName.toString());

    return Arrays.asList(
        metrics.currentThreads(
            () ->
                getNumberAttribute(objectName, "ExecuteThreadTotalCount")
                    .applyAsDouble(mBeanServer)),
        // WebLogic puts threads that are not needed to handle the present work load into a standby
        // pool. Include these in idle thread count.
        metrics.idleThreads(
            () ->
                getNumberAttribute(objectName, "ExecuteThreadIdleCount").applyAsDouble(mBeanServer)
                    + getNumberAttribute(objectName, "StandbyThreadCount")
                        .applyAsDouble(mBeanServer)),
        metrics.tasksCompleted(
            () ->
                getNumberAttribute(objectName, "CompletedRequestCount")
                    .applyAsDouble(mBeanServer)));
  }

  private WebLogicThreadPoolOtelMetrics() {}
}
