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

package com.splunk.opentelemetry.hikaricp;

import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_ACTIVE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_CREATE_TIME;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_IDLE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_IDLE_MIN;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_MAX;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_PENDING_THREADS;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_TIMEOUTS;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_TOTAL;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_USE_TIME;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_WAIT_TIME;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.POOL_NAME;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.POOL_TYPE;
import static java.util.Arrays.asList;

import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.PoolStats;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MicrometerMetricsTracker implements IMetricsTracker {
  private final Counter timeouts;
  private final Timer createTime;
  private final Timer waitTime;
  private final Timer useTime;
  private final List<Meter> allMeters;

  public MicrometerMetricsTracker(String poolName, PoolStats poolStats) {
    Tags tags = poolTags(poolName);

    timeouts = CONNECTIONS_TIMEOUTS.create(tags);
    createTime = CONNECTIONS_CREATE_TIME.create(tags);
    waitTime = CONNECTIONS_WAIT_TIME.create(tags);
    useTime = CONNECTIONS_USE_TIME.create(tags);

    allMeters =
        asList(
            timeouts,
            createTime,
            waitTime,
            useTime,
            CONNECTIONS_TOTAL.create(tags, poolStats::getTotalConnections),
            CONNECTIONS_ACTIVE.create(tags, poolStats::getActiveConnections),
            CONNECTIONS_IDLE.create(tags, poolStats::getIdleConnections),
            CONNECTIONS_IDLE_MIN.create(tags, poolStats::getMinConnections),
            CONNECTIONS_MAX.create(tags, poolStats::getMaxConnections),
            CONNECTIONS_PENDING_THREADS.create(tags, poolStats::getPendingThreads));
  }

  private static Tags poolTags(String poolName) {
    return Tags.of(Tag.of(POOL_TYPE, "hikari"), Tag.of(POOL_NAME, poolName));
  }

  @Override
  public void recordConnectionCreatedMillis(long connectionCreatedMillis) {
    createTime.record(connectionCreatedMillis, TimeUnit.MILLISECONDS);
  }

  @Override
  public void recordConnectionAcquiredNanos(long elapsedAcquiredNanos) {
    waitTime.record(elapsedAcquiredNanos, TimeUnit.NANOSECONDS);
  }

  @Override
  public void recordConnectionUsageMillis(long elapsedBorrowedMillis) {
    useTime.record(elapsedBorrowedMillis, TimeUnit.MILLISECONDS);
  }

  @Override
  public void recordConnectionTimeout() {
    timeouts.increment();
  }

  @Override
  public void close() {
    for (Meter meter : allMeters) {
      Metrics.globalRegistry.remove(meter);
    }
  }
}
