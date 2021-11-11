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

package com.splunk.opentelemetry.instrumentation.tomcat.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.apache.tomcat.util.net.AbstractEndpoint;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ThreadPoolSemanticConventions.EXECUTOR_NAME;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ThreadPoolSemanticConventions.EXECUTOR_TYPE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ThreadPoolSemanticConventions.TASKS_COMPLETED;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ThreadPoolSemanticConventions.TASKS_SUBMITTED;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ThreadPoolSemanticConventions.THREADS_ACTIVE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ThreadPoolSemanticConventions.THREADS_CORE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ThreadPoolSemanticConventions.THREADS_CURRENT;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ThreadPoolSemanticConventions.THREADS_IDLE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ThreadPoolSemanticConventions.THREADS_MAX;

public final class ThreadPoolMetrics {

  // a weak map does not make sense here because each Meter holds a reference to the executor
  // AbstractEndpoint does not implement equals()/hashCode(), so it's safe to keep them in a plain
  // ConcurrentHashMap
  private static final Map<AbstractEndpoint<?, ?>, List<Meter>> threadPoolMetrics =
      new ConcurrentHashMap<>();

  public static void registerMetrics(AbstractEndpoint<?, ?> endpoint) {
    threadPoolMetrics.computeIfAbsent(endpoint, ThreadPoolMetrics::createMeters);
  }

  private static List<Meter> createMeters(AbstractEndpoint<?, ?> endpoint) {
    Tags tags = executorTags(endpoint);

    Suppliers suppliers = Suppliers.fromEndpoint(endpoint);
    return Arrays.asList(
        THREADS_CURRENT.create(tags, suppliers::getCurrentThreads),
        THREADS_ACTIVE.create(tags, suppliers::getActiveThreads),
        THREADS_IDLE.create(tags, suppliers::getIdleThreads),
        THREADS_CORE.create(tags, suppliers::getCoreThreads),
        THREADS_MAX.create(tags, suppliers::getMaxThreads),
        TASKS_SUBMITTED.create(tags, suppliers::getSubmittedTasks),
        TASKS_COMPLETED.create(tags, suppliers::getCompletedTasks));
  }

  public static void unregisterMetrics(AbstractEndpoint<?, ?> endpoint) {
    List<Meter> meters = threadPoolMetrics.remove(endpoint);
    if (meters != null) {
      for (Meter meter : meters) {
        Metrics.globalRegistry.remove(meter);
      }
    }
  }

  private static Tags executorTags(AbstractEndpoint<?, ?> endpoint) {
    return Tags.of(Tag.of(EXECUTOR_TYPE, "tomcat"), Tag.of(EXECUTOR_NAME, endpoint.getName()));
  }

  private ThreadPoolMetrics() {}
}
