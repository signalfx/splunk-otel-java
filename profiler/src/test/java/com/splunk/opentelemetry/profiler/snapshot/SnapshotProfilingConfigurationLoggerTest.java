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

package com.splunk.opentelemetry.profiler.snapshot;

import com.splunk.opentelemetry.profiler.Configuration;
import io.github.netmikey.logunit.api.LogCapturer;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SnapshotProfilingConfigurationLoggerTest {
  @RegisterExtension
  private final LogCapturer log =
      LogCapturer.create().captureForType(SnapshotProfilingConfigurationLogger.class);

  @Test
  void includeSnapshotProfilingHeading() {
    var properties = DefaultConfigProperties.create(Collections.emptyMap());

    SnapshotProfilingConfigurationLogger.log(properties);

    log.assertContains("-----------------------");
    log.assertContains("Snapshot Profiler Configuration:");
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void includeSnapshotProfilingEnabled(boolean enabled) {
    var properties =
        DefaultConfigProperties.create(
            Map.of(Configuration.CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER, String.valueOf(enabled)));
    SnapshotProfilingConfigurationLogger.log(properties);
    log.assertContains(Configuration.CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER + " : " + enabled);
  }

  @ParameterizedTest
  @ValueSource(doubles = {.01, .05, .1})
  void includeSnapshotProfilingSelectionRate(double rate) {
    var properties =
        DefaultConfigProperties.create(
            Map.of(Configuration.CONFIG_KEY_SNAPSHOT_SELECTION_RATE, String.valueOf(rate)));
    SnapshotProfilingConfigurationLogger.log(properties);
    log.assertContains(Configuration.CONFIG_KEY_SNAPSHOT_SELECTION_RATE + " : " + rate);
  }

  @ParameterizedTest
  @ValueSource(ints = {100, 1000, 10_1000})
  void includeSnapshotProfilingStackTraceDepth(int depth) {
    var properties =
        DefaultConfigProperties.create(
            Map.of(Configuration.CONFIG_KEY_SNAPSHOT_PROFILER_STACK_DEPTH, String.valueOf(depth)));
    SnapshotProfilingConfigurationLogger.log(properties);
    log.assertContains(Configuration.CONFIG_KEY_SNAPSHOT_PROFILER_STACK_DEPTH + " : " + depth);
  }

  @ParameterizedTest
  @ValueSource(strings = {"5ms", "10ms", "100ms"})
  void includeSnapshotProfilingSamplingInterval(String interval) {
    var properties =
        DefaultConfigProperties.create(
            Map.of(Configuration.CONFIG_KEY_SNAPSHOT_PROFILER_SAMPLING_INTERVAL, interval));

    SnapshotProfilingConfigurationLogger.log(properties);

    var duration =
        properties.getDuration(Configuration.CONFIG_KEY_SNAPSHOT_PROFILER_SAMPLING_INTERVAL);
    log.assertContains(
        Configuration.CONFIG_KEY_SNAPSHOT_PROFILER_SAMPLING_INTERVAL + " : " + duration);
  }

  @ParameterizedTest
  @ValueSource(strings = {"10s", "30s", "10m"})
  void includeSnapshotProfilingExportInterval(String interval) {
    var properties =
        DefaultConfigProperties.create(
            Map.of(Configuration.CONFIG_KEY_SNAPSHOT_PROFILER_EXPORT_INTERVAL, interval));

    SnapshotProfilingConfigurationLogger.log(properties);

    var duration =
        properties.getDuration(Configuration.CONFIG_KEY_SNAPSHOT_PROFILER_EXPORT_INTERVAL);
    log.assertContains(
        Configuration.CONFIG_KEY_SNAPSHOT_PROFILER_EXPORT_INTERVAL + " : " + duration);
  }

  @ParameterizedTest
  @ValueSource(ints = {1000, 2000, 10_1000})
  void includeSnapshotProfilingStagingCapacity(int capacity) {
    var properties =
        DefaultConfigProperties.create(
            Map.of(
                Configuration.CONFIG_KEY_SNAPSHOT_PROFILER_STAGING_CAPACITY,
                String.valueOf(capacity)));
    SnapshotProfilingConfigurationLogger.log(properties);
    log.assertContains(
        Configuration.CONFIG_KEY_SNAPSHOT_PROFILER_STAGING_CAPACITY + " : " + capacity);
  }
}
