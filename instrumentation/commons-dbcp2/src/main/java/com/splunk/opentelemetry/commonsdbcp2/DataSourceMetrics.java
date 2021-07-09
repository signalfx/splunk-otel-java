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

import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_ACTIVE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_IDLE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_IDLE_MAX;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_IDLE_MIN;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_MAX;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_TOTAL;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.POOL_NAME;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.POOL_TYPE;

import com.splunk.opentelemetry.javaagent.bootstrap.metrics.GlobalMetricsTags;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import javax.management.ObjectName;
import org.apache.commons.dbcp2.BasicDataSourceMXBean;

public final class DataSourceMetrics {

  // a weak map does not make sense here because each Meter holds a reference to the dataSource
  // all instrumented/known implementations of BasicDataSourceMXBean do not implement
  // equals()/hashCode(), so it's safe to keep them in a plain ConcurrentHashMap
  private static final Map<BasicDataSourceMXBean, List<Meter>> dataSourceMetrics =
      new ConcurrentHashMap<>();

  public static void registerMetrics(BasicDataSourceMXBean dataSource, ObjectName objectName) {
    Tags tags = poolTags(objectName);

    List<Meter> meters =
        Arrays.asList(
            CONNECTIONS_TOTAL.create(tags, new TotalConnectionsUsed(dataSource)),
            CONNECTIONS_ACTIVE.create(tags, dataSource::getNumActive),
            CONNECTIONS_IDLE.create(tags, dataSource::getNumIdle),
            CONNECTIONS_IDLE_MIN.create(tags, dataSource::getMinIdle),
            CONNECTIONS_IDLE_MAX.create(tags, dataSource::getMaxIdle),
            CONNECTIONS_MAX.create(tags, dataSource::getMaxTotal));
    dataSourceMetrics.put(dataSource, meters);
  }

  public static void unregisterMetrics(BasicDataSourceMXBean dataSource) {
    List<Meter> meters = dataSourceMetrics.remove(dataSource);
    for (Meter meter : meters) {
      Metrics.globalRegistry.remove(meter);
    }
  }

  private static Tags poolTags(ObjectName objectName) {
    // use the "name" property if available: Spring sets it to the bean name
    String name = objectName.getKeyProperty("name");
    // if its unavailable just use the whole mbean name
    if (name == null) {
      name = objectName.toString();
    }
    return GlobalMetricsTags.get().and(Tag.of(POOL_TYPE, "dbcp2"), Tag.of(POOL_NAME, name));
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
