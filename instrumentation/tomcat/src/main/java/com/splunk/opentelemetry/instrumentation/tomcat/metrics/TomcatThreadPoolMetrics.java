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

import static java.util.logging.Level.WARNING;

import com.splunk.opentelemetry.javaagent.bootstrap.metrics.otel.ThreadPoolMetrics;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.apache.tomcat.util.net.AbstractEndpoint;

public final class TomcatThreadPoolMetrics {
  private static final Logger logger = Logger.getLogger(TomcatThreadPoolMetrics.class.getName());

  private static final String INSTRUMENTATION_NAME =
      "com.splunk.javaagent.tomcat-thread-pool-metrics";

  // a weak map does not make sense here because each Meter holds a reference to the executor
  // AbstractEndpoint does not implement equals()/hashCode(), so it's safe to keep them in a plain
  // ConcurrentHashMap
  private static final Map<AbstractEndpoint<?, ?>, List<AutoCloseable>> threadPoolMetrics =
      new ConcurrentHashMap<>();

  public static void registerMetrics(AbstractEndpoint<?, ?> endpoint) {
    threadPoolMetrics.computeIfAbsent(endpoint, TomcatThreadPoolMetrics::createMeters);
  }

  private static List<AutoCloseable> createMeters(AbstractEndpoint<?, ?> endpoint) {
    OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
    ThreadPoolMetrics metrics =
        ThreadPoolMetrics.create(openTelemetry, INSTRUMENTATION_NAME, "tomcat", endpoint.getName());

    LateBindingTomcatThreadPoolMetrics tomcatThreadPoolMetrics =
        new LateBindingTomcatThreadPoolMetrics(endpoint);
    return Arrays.asList(
        metrics.currentThreads(tomcatThreadPoolMetrics::getCurrentThreads),
        metrics.activeThreads(tomcatThreadPoolMetrics::getActiveThreads),
        metrics.idleThreads(tomcatThreadPoolMetrics::getIdleThreads),
        metrics.coreThreads(tomcatThreadPoolMetrics::getCoreThreads),
        metrics.maxThreads(tomcatThreadPoolMetrics::getMaxThreads),
        metrics.tasksSubmitted(tomcatThreadPoolMetrics::getSubmittedTasks),
        metrics.tasksCompleted(tomcatThreadPoolMetrics::getCompletedTasks));
  }

  public static void unregisterMetrics(AbstractEndpoint<?, ?> endpoint) {
    List<AutoCloseable> observableInstruments = threadPoolMetrics.remove(endpoint);
    if (observableInstruments != null) {
      for (AutoCloseable observable : observableInstruments) {
        try {
          observable.close();
        } catch (Exception exception) {
          logger.log(WARNING, "Failed to close instrument", exception);
        }
      }
    }
  }

  private TomcatThreadPoolMetrics() {}
}
