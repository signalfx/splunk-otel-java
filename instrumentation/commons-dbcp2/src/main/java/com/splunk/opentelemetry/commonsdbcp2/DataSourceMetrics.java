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
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.BaseUnits;
import java.util.List;
import java.util.function.Supplier;
import javax.management.ObjectName;
import org.apache.commons.dbcp2.BasicDataSourceMXBean;

public final class DataSourceMetrics {
  public static void registerMetrics(BasicDataSourceMXBean dataSource, ObjectName objectName) {
    List<Tag> tags = getTags(objectName);

    gauge("db.pool.connections.active", tags, dataSource::getNumActive);
    gauge("db.pool.connections.idle", tags, dataSource::getNumIdle);
    gauge("db.pool.connections.min", tags, dataSource::getMinIdle);
    gauge("db.pool.connections.max", tags, dataSource::getMaxTotal);
  }

  private static List<Tag> getTags(ObjectName objectName) {
    // use the "name" property if available: Spring sets it to the bean name
    String name = objectName.getKeyProperty("name");
    // if its unavailable just use the whole mbean name
    if (name == null) {
      name = objectName.toString();
    }
    return GlobalMetricsTags.concat(Tag.of("type", "dbcp2"), Tag.of("name", name));
  }

  private static void gauge(String name, Iterable<Tag> tags, Supplier<Number> function) {
    Gauge.builder(name, function)
        .tags(tags)
        .baseUnit(BaseUnits.CONNECTIONS)
        .register(Metrics.globalRegistry);
  }

  private DataSourceMetrics() {}
}
