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

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.time.Duration;
import java.util.logging.Logger;

public final class SnapshotProfilingEnvVarsConfigurationFactory {
  private static final Logger logger =
      Logger.getLogger(SnapshotProfilingEnvVarsConfigurationFactory.class.getName());

  // Visible for tests
  static final String CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER = "splunk.snapshot.profiler.enabled";
  static final String SELECTION_PROBABILITY_KEY = "splunk.snapshot.selection.probability";
  static final String STACK_DEPTH_KEY = "splunk.snapshot.profiler.max.stack.depth";
  static final String SAMPLING_INTERVAL_KEY = "splunk.snapshot.sampling.interval";
  static final String EXPORT_INTERVAL_KEY = "splunk.snapshot.profiler.export.interval";
  static final String STAGING_CAPACITY_KEY = "splunk.snapshot.profiler.staging.capacity";

  private SnapshotProfilingEnvVarsConfigurationFactory() {}

  public static SnapshotProfilingConfiguration create(ConfigProperties properties) {
    return SnapshotProfilingConfiguration.builder()
        .setEnabled(properties.getBoolean(CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER, false))
        .setSnapshotSelectionProbability(getSnapshotSelectionProbability(properties))
        .setStackDepth(
            properties.getInt(STACK_DEPTH_KEY, SnapshotProfilingConfiguration.DEFAULT_STACK_DEPTH))
        .setSamplingInterval(
            properties.getDuration(
                SAMPLING_INTERVAL_KEY,
                Duration.ofMillis(SnapshotProfilingConfiguration.DEFAULT_SAMPLING_INTERVAL)))
        .setExportInterval(
            properties.getDuration(
                EXPORT_INTERVAL_KEY,
                Duration.ofMillis(SnapshotProfilingConfiguration.DEFAULT_EXPORT_INTERVAL)))
        .setStagingCapacity(
            properties.getInt(
                STAGING_CAPACITY_KEY, SnapshotProfilingConfiguration.DEFAULT_STAGING_CAPACITY))
        .setConfigProperties(properties)
        .build();
  }

  private static double getSnapshotSelectionProbability(ConfigProperties properties) {
    String selectionProbabilityPropertyValue =
        properties.getString(
            SELECTION_PROBABILITY_KEY,
            String.valueOf(SnapshotProfilingConfiguration.DEFAULT_SELECTION_PROBABILITY));
    try {
      double selectionProbability = Double.parseDouble(selectionProbabilityPropertyValue);
      return SnapshotProfilingConfiguration.validateSelectionProbability(
          selectionProbability, logger);
    } catch (NumberFormatException e) {
      logger.warning(
          "Invalid snapshot selection probability: '"
              + selectionProbabilityPropertyValue
              + "', using default probability of '"
              + SnapshotProfilingConfiguration.DEFAULT_SELECTION_PROBABILITY
              + "'");
      return SnapshotProfilingConfiguration.DEFAULT_SELECTION_PROBABILITY;
    }
  }
}
