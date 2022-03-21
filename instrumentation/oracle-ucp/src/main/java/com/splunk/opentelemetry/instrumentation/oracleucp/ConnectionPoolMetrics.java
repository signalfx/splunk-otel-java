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

package com.splunk.opentelemetry.instrumentation.oracleucp;

import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ConnectionPoolSemanticConventions.CONNECTIONS_ACTIVE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ConnectionPoolSemanticConventions.CONNECTIONS_IDLE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ConnectionPoolSemanticConventions.CONNECTIONS_MAX;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ConnectionPoolSemanticConventions.CONNECTIONS_PENDING_THREADS;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ConnectionPoolSemanticConventions.CONNECTIONS_TOTAL;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ConnectionPoolSemanticConventions.POOL_NAME;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ConnectionPoolSemanticConventions.POOL_TYPE;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import oracle.ucp.UniversalConnectionPool;

public final class ConnectionPoolMetrics {

  // a weak map does not make sense here because each Meter holds a reference to the connectionPool
  // none of the UniversalConnectionPool implementations contain equals()/hashCode(), so it's safe
  // to keep them in a plain ConcurrentHashMap
  private static final Map<UniversalConnectionPool, List<Meter>> dataSourceMetrics =
      new ConcurrentHashMap<>();

  public static void registerMetrics(UniversalConnectionPool connectionPool) {
    dataSourceMetrics.computeIfAbsent(connectionPool, ConnectionPoolMetrics::createMeters);
  }

  private static List<Meter> createMeters(UniversalConnectionPool ucp) {
    Tags tags = poolTags(ucp);

    return Arrays.asList(
        CONNECTIONS_TOTAL.create(tags, () -> ucp.getStatistics().getTotalConnectionsCount()),
        CONNECTIONS_ACTIVE.create(tags, ucp::getBorrowedConnectionsCount),
        CONNECTIONS_IDLE.create(tags, ucp::getAvailableConnectionsCount),
        CONNECTIONS_MAX.create(tags, () -> ucp.getStatistics().getPeakConnectionsCount()),
        CONNECTIONS_PENDING_THREADS.create(
            tags, () -> ucp.getStatistics().getPendingRequestsCount()));
  }

  public static void unregisterMetrics(UniversalConnectionPool connectionPool) {
    List<Meter> meters = dataSourceMetrics.remove(connectionPool);
    if (meters != null) {
      for (Meter meter : meters) {
        Metrics.globalRegistry.remove(meter);
      }
    }
  }

  private static Tags poolTags(UniversalConnectionPool connectionPool) {
    return Tags.of(Tag.of(POOL_TYPE, "oracle-ucp"), Tag.of(POOL_NAME, connectionPool.getName()));
  }

  private ConnectionPoolMetrics() {}
}
