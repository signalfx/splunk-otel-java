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

class SnapshotProfilingEnvVarsConfiguration implements SnapshotProfilingConfiguration {
  private static final Logger logger =
      Logger.getLogger(SnapshotProfilingEnvVarsConfiguration.class.getName());

  private final ConfigProperties properties;

  // Visible for tests
  static final String CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER = "splunk.snapshot.profiler.enabled";
  static final String SELECTION_PROBABILITY_KEY = "splunk.snapshot.selection.probability";
  static final String STACK_DEPTH_KEY = "splunk.snapshot.profiler.max.stack.depth";
  static final String SAMPLING_INTERVAL_KEY = "splunk.snapshot.sampling.interval";
  static final String EXPORT_INTERVAL_KEY = "splunk.snapshot.profiler.export.interval";
  static final String STAGING_CAPACITY_KEY = "splunk.snapshot.profiler.staging.capacity";

  SnapshotProfilingEnvVarsConfiguration(ConfigProperties properties) {
    this.properties = properties;
  }

  @Override
  public void log() {
    logger.fine("Snapshot Profiler Configuration:");
    logger.fine("-------------------------------------------------------");

    log(CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER, isEnabled());
    log(SELECTION_PROBABILITY_KEY, getSnapshotSelectionProbability());
    log(STACK_DEPTH_KEY, getStackDepth());
    log(SAMPLING_INTERVAL_KEY, getSamplingInterval().toMillis() + "ms");
    log(EXPORT_INTERVAL_KEY, getExportInterval().toMillis() + "ms");
    log(STAGING_CAPACITY_KEY, getStagingCapacity());
    logger.fine("-------------------------------------------------------");
  }

  @Override
  public boolean isEnabled() {
    return properties.getBoolean(CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER, false);
  }

  @Override
  public double getSnapshotSelectionProbability() {
    String selectionProbabilityPropertyValue =
        properties.getString(
            SELECTION_PROBABILITY_KEY, String.valueOf(DEFAULT_SELECTION_PROBABILITY));
    try {
      double selectionProbability = Double.parseDouble(selectionProbabilityPropertyValue);
      return SnapshotProfilingConfiguration.validateSelectionProbability(
          selectionProbability, logger);
    } catch (NumberFormatException e) {
      logger.warning(
          "Invalid snapshot selection probability: '"
              + selectionProbabilityPropertyValue
              + "', using default probability of '"
              + DEFAULT_SELECTION_PROBABILITY
              + "'");
      return DEFAULT_SELECTION_PROBABILITY;
    }
  }

  @Override
  public int getStackDepth() {
    return properties.getInt(STACK_DEPTH_KEY, DEFAULT_STACK_DEPTH);
  }

  @Override
  public Duration getSamplingInterval() {
    return properties.getDuration(
        SAMPLING_INTERVAL_KEY, Duration.ofMillis(DEFAULT_SAMPLING_INTERVAL));
  }

  @Override
  public Duration getExportInterval() {
    return properties.getDuration(EXPORT_INTERVAL_KEY, Duration.ofMillis(DEFAULT_EXPORT_INTERVAL));
  }

  @Override
  public int getStagingCapacity() {
    return properties.getInt(STAGING_CAPACITY_KEY, DEFAULT_STAGING_CAPACITY);
  }

  @Override
  public Object getConfigProperties() {
    return properties;
  }

  private void log(String key, Object value) {
    logger.fine(" " + pad(key) + " : " + value);
  }

  private String pad(String str) {
    return String.format("%42s", str);
  }
}
