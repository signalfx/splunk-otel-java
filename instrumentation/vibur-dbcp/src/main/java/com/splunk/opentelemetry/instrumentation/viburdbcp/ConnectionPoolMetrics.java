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

package com.splunk.opentelemetry.instrumentation.viburdbcp;

import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ConnectionPoolSemanticConventions.CONNECTIONS_ACTIVE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ConnectionPoolSemanticConventions.CONNECTIONS_IDLE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ConnectionPoolSemanticConventions.CONNECTIONS_MAX;
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
import org.vibur.dbcp.ViburDBCPDataSource;

public final class ConnectionPoolMetrics {

  // a weak map does not make sense here because each Meter holds a reference to the dataSource
  // ViburDBCPDataSource does not implement equals()/hashCode(), so it's safe to keep them in a
  // plain ConcurrentHashMap
  private static final Map<ViburDBCPDataSource, List<Meter>> dataSourceMetrics =
      new ConcurrentHashMap<>();

  public static void registerMetrics(ViburDBCPDataSource dataSource) {
    dataSourceMetrics.computeIfAbsent(dataSource, ConnectionPoolMetrics::createMeters);
  }

  private static List<Meter> createMeters(ViburDBCPDataSource dataSource) {
    Tags tags = poolTags(dataSource);

    return Arrays.asList(
        CONNECTIONS_TOTAL.create(tags, () -> dataSource.getPool().createdTotal()),
        CONNECTIONS_ACTIVE.create(tags, () -> dataSource.getPool().taken()),
        CONNECTIONS_IDLE.create(tags, () -> dataSource.getPool().remainingCreated()),
        CONNECTIONS_MAX.create(tags, dataSource::getPoolMaxSize));
  }

  public static void unregisterMetrics(ViburDBCPDataSource dataSource) {
    List<Meter> meters = dataSourceMetrics.remove(dataSource);
    if (meters != null) {
      for (Meter meter : meters) {
        Metrics.globalRegistry.remove(meter);
      }
    }
  }

  private static Tags poolTags(ViburDBCPDataSource dataSource) {
    return Tags.of(Tag.of(POOL_TYPE, "vibur-dbcp"), Tag.of(POOL_NAME, dataSource.getName()));
  }

  private ConnectionPoolMetrics() {}
}
