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
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import java.util.List;
import javax.management.ObjectName;
import org.apache.commons.dbcp2.BasicDataSourceMXBean;

public final class DataSourceMetrics {
  public static void registerMetrics(BasicDataSourceMXBean dataSource, ObjectName objectName) {
    // use the "name" property if available: Spring sets it to the bean name
    String name = objectName.getKeyProperty("name");
    // if its unavailable just use the whole mbean name
    if (name == null) {
      name = objectName.toString();
    }
    List<Tag> tags = GlobalMetricsTags.concat(Tag.of("type", "dbcp2"), Tag.of("name", name));

    Metrics.gauge(
        "db.pool.connections.active", tags, dataSource, BasicDataSourceMXBean::getNumActive);
    Metrics.gauge("db.pool.connections.idle", tags, dataSource, BasicDataSourceMXBean::getNumIdle);
    Metrics.gauge("db.pool.connections.min", tags, dataSource, BasicDataSourceMXBean::getMinIdle);
    Metrics.gauge("db.pool.connections.max", tags, dataSource, BasicDataSourceMXBean::getMaxTotal);
  }

  private DataSourceMetrics() {}
}
