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

package com.splunk.opentelemetry.c3p0;

import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_ACTIVE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_IDLE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_PENDING_THREADS;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_TOTAL;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.POOL_NAME;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.POOL_TYPE;

import com.mchange.v2.c3p0.impl.AbstractPoolBackedDataSource;
import com.splunk.opentelemetry.javaagent.bootstrap.metrics.GaugeSemanticConvention;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class ConnectionPoolMetrics {

  // a weak map does not make sense here because each Meter holds a reference to the dataSource
  // DataSourceProxy does not implement equals()/hashCode(), so it's safe to keep them in a plain
  // ConcurrentHashMap
  private static final Map<AbstractPoolBackedDataSource, List<Meter>> dataSourceMetrics =
      new ConcurrentHashMap<>();

  public static void registerMetrics(AbstractPoolBackedDataSource dataSource) {
    dataSourceMetrics.computeIfAbsent(dataSource, ConnectionPoolMetrics::createMeters);
  }

  private static List<Meter> createMeters(AbstractPoolBackedDataSource dataSource) {
    Tags tags = poolTags(dataSource);

    return Arrays.asList(
        create(CONNECTIONS_TOTAL, tags, dataSource::getNumConnectionsDefaultUser),
        create(CONNECTIONS_ACTIVE, tags, dataSource::getNumBusyConnectionsDefaultUser),
        create(CONNECTIONS_IDLE, tags, dataSource::getNumIdleConnectionsDefaultUser),
        create(
            CONNECTIONS_PENDING_THREADS,
            tags,
            dataSource::getNumThreadsAwaitingCheckoutDefaultUser));
  }

  public static void unregisterMetrics(AbstractPoolBackedDataSource dataSource) {
    List<Meter> meters = dataSourceMetrics.remove(dataSource);
    if (meters != null) {
      for (Meter meter : meters) {
        Metrics.globalRegistry.remove(meter);
      }
    }
  }

  private static Tags poolTags(AbstractPoolBackedDataSource dataSource) {
    return Tags.of(Tag.of(POOL_TYPE, "c3p0"), Tag.of(POOL_NAME, dataSource.getDataSourceName()));
  }

  private static Meter create(
      GaugeSemanticConvention convention, Tags tags, SqlExceptionHandlingSupplier supplier) {
    return convention.create(tags, supplier);
  }

  @FunctionalInterface
  public interface SqlExceptionHandlingSupplier extends Supplier<Number> {

    Number unsafeGet() throws SQLException;

    @Override
    default Number get() {
      try {
        return unsafeGet();
      } catch (SQLException e) {
        return Double.NaN;
      }
    }
  }

  private ConnectionPoolMetrics() {}
}
