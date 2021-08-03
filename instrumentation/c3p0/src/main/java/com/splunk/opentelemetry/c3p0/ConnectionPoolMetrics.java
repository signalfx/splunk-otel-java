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
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ConnectionPoolMetrics {

  // a weak map does not make sense here because each Meter holds a reference to the dataSource
  // AbstractPoolBackedDataSource implements equals() & hashCode() in IdentityTokenResolvable,
  // that's why we wrap it with IdentityDataSourceKey that uses identity comparison instead
  private static final Map<IdentityDataSourceKey, List<Meter>> dataSourceMetrics =
      new ConcurrentHashMap<>();

  public static void registerMetrics(AbstractPoolBackedDataSource dataSource) {
    dataSourceMetrics.compute(
        new IdentityDataSourceKey(dataSource), ConnectionPoolMetrics::createMeters);
  }

  private static List<Meter> createMeters(IdentityDataSourceKey key, List<Meter> oldMeters) {
    // remove old meters from the registry in case they were already there
    removeMetersFromRegistry(oldMeters);

    AbstractPoolBackedDataSource dataSource = key.dataSource;
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
    List<Meter> meters = dataSourceMetrics.remove(new IdentityDataSourceKey(dataSource));
    removeMetersFromRegistry(meters);
  }

  private static void removeMetersFromRegistry(@Nullable List<Meter> meters) {
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

  /**
   * A wrapper over {@link AbstractPoolBackedDataSource} that implements identity comparison in its
   * {@link #equals(Object)} and {@link #hashCode()} methods.
   */
  static final class IdentityDataSourceKey {
    final AbstractPoolBackedDataSource dataSource;

    IdentityDataSourceKey(AbstractPoolBackedDataSource dataSource) {
      this.dataSource = dataSource;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      IdentityDataSourceKey that = (IdentityDataSourceKey) o;
      return dataSource == that.dataSource;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(dataSource);
    }
  }

  @FunctionalInterface
  interface SqlExceptionHandlingSupplier extends Supplier<Number> {

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
