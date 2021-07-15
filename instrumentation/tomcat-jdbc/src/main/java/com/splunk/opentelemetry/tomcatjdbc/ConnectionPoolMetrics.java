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

package com.splunk.opentelemetry.tomcatjdbc;

import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_ACTIVE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_IDLE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_IDLE_MAX;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_IDLE_MIN;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_MAX;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_PENDING_THREADS;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_TOTAL;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.POOL_NAME;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.POOL_TYPE;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.tomcat.jdbc.pool.jmx.ConnectionPool;

public final class ConnectionPoolMetrics {

  // a weak map does not make sense here because each Meter holds a reference to the connectionPool
  // ConnectionPool does not implement equals()/hashCode(), so it's safe to keep them in a plain
  // ConcurrentHashMap
  private static final Map<ConnectionPool, List<Meter>> connectionPoolMetrics =
      new ConcurrentHashMap<>();

  public static void registerMetrics(ConnectionPool connectionPool) {
    Tags tags = poolTags(connectionPool);

    List<Meter> meters =
        Arrays.asList(
            CONNECTIONS_TOTAL.create(tags, connectionPool::getSize),
            CONNECTIONS_ACTIVE.create(tags, connectionPool::getActive),
            CONNECTIONS_IDLE.create(tags, connectionPool::getIdle),
            CONNECTIONS_IDLE_MIN.create(tags, connectionPool::getMinIdle),
            CONNECTIONS_IDLE_MAX.create(tags, connectionPool::getMaxIdle),
            CONNECTIONS_MAX.create(tags, connectionPool::getMaxActive),
            CONNECTIONS_PENDING_THREADS.create(tags, connectionPool::getWaitCount));
    connectionPoolMetrics.put(connectionPool, meters);
  }

  public static void unregisterMetrics(ConnectionPool connectionPool) {
    List<Meter> meters = connectionPoolMetrics.remove(connectionPool);
    for (Meter meter : meters) {
      Metrics.globalRegistry.remove(meter);
    }
  }

  private static Tags poolTags(ConnectionPool connectionPool) {
    return Tags.of(
        Tag.of(POOL_TYPE, "tomcat-jdbc"), Tag.of(POOL_NAME, connectionPool.getPoolName()));
  }

  private ConnectionPoolMetrics() {}
}
