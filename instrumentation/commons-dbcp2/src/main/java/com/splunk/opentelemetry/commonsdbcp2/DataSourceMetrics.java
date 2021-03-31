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

package com.splunk.opentelemetry.commonsdbcp2;

import com.splunk.opentelemetry.javaagent.bootstrap.GlobalMetricsTags;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.BaseUnits;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import javax.management.ObjectName;
import org.apache.commons.dbcp2.BasicDataSourceMXBean;

public final class DataSourceMetrics {

  /** The number of open connections. */
  private static final String CONNECTIONS_TOTAL = "db.pool.connections";
  /** The number of open connections that are currently in use. */
  private static final String CONNECTIONS_ACTIVE = "db.pool.connections.active";
  /** The number of open connections that are currently idle. */
  private static final String CONNECTIONS_IDLE = "db.pool.connections.idle";
  /** The minimum number of idle open connections allowed. */
  private static final String CONNECTIONS_IDLE_MIN = "db.pool.connections.idle.min";
  /** The maximum number of idle open connections allowed. */
  private static final String CONNECTIONS_IDLE_MAX = "db.pool.connections.idle.max";
  /** The maximum number of open connections allowed. */
  private static final String CONNECTIONS_MAX = "db.pool.connections.max";

  // a weak map does not make sense here because each Meter holds a reference to the dataSource
  private static final Map<BasicDataSourceMXBean, List<Meter>> dataSourceMetrics =
      new ConcurrentHashMap<>();

  public static void registerMetrics(BasicDataSourceMXBean dataSource, ObjectName objectName) {
    Tags tags = getTags(objectName);

    List<Meter> meters =
        Arrays.asList(
            gauge(CONNECTIONS_TOTAL, tags, new TotalConnectionsUsed(dataSource)),
            gauge(CONNECTIONS_ACTIVE, tags, dataSource::getNumActive),
            gauge(CONNECTIONS_IDLE, tags, dataSource::getNumIdle),
            gauge(CONNECTIONS_IDLE_MIN, tags, dataSource::getMinIdle),
            gauge(CONNECTIONS_IDLE_MAX, tags, dataSource::getMaxIdle),
            gauge(CONNECTIONS_MAX, tags, dataSource::getMaxTotal));
    dataSourceMetrics.put(dataSource, meters);
  }

  public static void unregisterMetrics(BasicDataSourceMXBean dataSource) {
    List<Meter> meters = dataSourceMetrics.remove(dataSource);
    for (Meter meter : meters) {
      Metrics.globalRegistry.remove(meter);
    }
  }

  private static Tags getTags(ObjectName objectName) {
    // use the "name" property if available: Spring sets it to the bean name
    String name = objectName.getKeyProperty("name");
    // if its unavailable just use the whole mbean name
    if (name == null) {
      name = objectName.toString();
    }
    return GlobalMetricsTags.get().and(Tag.of("pool.type", "dbcp2"), Tag.of("pool.name", name));
  }

  private static Meter gauge(String name, Iterable<Tag> tags, Supplier<Number> function) {
    return Gauge.builder(name, function)
        .tags(tags)
        .baseUnit(BaseUnits.CONNECTIONS)
        .register(Metrics.globalRegistry);
  }

  private static final class TotalConnectionsUsed implements Supplier<Number> {
    private final BasicDataSourceMXBean dataSource;

    private TotalConnectionsUsed(BasicDataSourceMXBean dataSource) {
      this.dataSource = dataSource;
    }

    @Override
    public Number get() {
      return dataSource.getNumIdle() + dataSource.getNumActive();
    }
  }

  private DataSourceMetrics() {}
}
