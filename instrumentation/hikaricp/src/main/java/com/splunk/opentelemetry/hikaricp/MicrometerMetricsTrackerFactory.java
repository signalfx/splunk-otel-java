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

import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import com.zaxxer.hikari.metrics.PoolStats;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MicrometerMetricsTrackerFactory implements MetricsTrackerFactory {
  @Nullable private final MetricsTrackerFactory userMetricsFactory;

  public MicrometerMetricsTrackerFactory(@Nullable MetricsTrackerFactory userMetricsFactory) {
    this.userMetricsFactory = userMetricsFactory;
  }

  @Override
  public IMetricsTracker create(String poolName, PoolStats poolStats) {
    IMetricsTracker userMetrics =
        userMetricsFactory == null ? null : userMetricsFactory.create(poolName, poolStats);
    return new MicrometerMetricsTracker(userMetrics, poolName, poolStats);
  }
}
