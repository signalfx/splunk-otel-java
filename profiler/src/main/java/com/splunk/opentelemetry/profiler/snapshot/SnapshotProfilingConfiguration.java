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

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class SnapshotProfilingConfiguration implements AutoConfigurationCustomizerProvider {
  private static final Logger logger =
      Logger.getLogger(SnapshotProfilingConfiguration.class.getName());

  public static final String CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER =
      "splunk.snapshot.profiler.enabled";
  private static final String SELECTION_PROBABILITY_KEY = "splunk.snapshot.selection.probability";
  private static final String STACK_DEPTH_KEY = "splunk.snapshot.profiler.max.stack.depth";
  private static final String SAMPLING_INTERVAL_KEY = "splunk.snapshot.sampling.interval";
  private static final String EXPORT_INTERVAL_KEY = "splunk.snapshot.profiler.export.interval";
  private static final String STAGING_CAPACITY_KEY = "splunk.snapshot.profiler.staging.capacity";

  private static final double DEFAULT_SELECTION_PROBABILITY = 0.01;
  private static final double MAX_SELECTION_PROBABILITY = 0.10;
  private static final int DEFAULT_STACK_DEPTH = 1024;
  private static final String DEFAULT_SAMPLING_INTERVAL_STRING = "10ms";
  private static final Duration DEFAULT_SAMPLING_INTERVAL = Duration.ofMillis(10);
  private static final String DEFAULT_EXPORT_INTERVAL_STRING = "5s";
  private static final Duration DEFAULT_EXPORT_INTERVAL = Duration.ofSeconds(5);
  private static final int DEFAULT_STAGING_CAPACITY = 2000;

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addPropertiesSupplier(
        () -> {
          Map<String, String> map = new HashMap<>();
          map.put(CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER, String.valueOf(false));
          map.put(SELECTION_PROBABILITY_KEY, String.valueOf(DEFAULT_SELECTION_PROBABILITY));
          map.put(STACK_DEPTH_KEY, String.valueOf(DEFAULT_STACK_DEPTH));
          map.put(SAMPLING_INTERVAL_KEY, DEFAULT_SAMPLING_INTERVAL_STRING);
          map.put(EXPORT_INTERVAL_KEY, DEFAULT_EXPORT_INTERVAL_STRING);
          map.put(STAGING_CAPACITY_KEY, String.valueOf(DEFAULT_STAGING_CAPACITY));
          return map;
        });
  }

  static void log(ConfigProperties properties) {
    logger.fine("Snapshot Profiler Configuration:");
    logger.fine("-------------------------------------------------------");

    log(CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER, isSnapshotProfilingEnabled(properties));
    log(SELECTION_PROBABILITY_KEY, getSnapshotSelectionProbability(properties));
    log(STACK_DEPTH_KEY, getStackDepth(properties));
    log(SAMPLING_INTERVAL_KEY, getSamplingInterval(properties));
    log(EXPORT_INTERVAL_KEY, getExportInterval(properties));
    log(STAGING_CAPACITY_KEY, getStagingCapacity(properties));
    logger.fine("-------------------------------------------------------");
  }

  static boolean isSnapshotProfilingEnabled(ConfigProperties properties) {
    return properties.getBoolean(CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER, false);
  }

  static double getSnapshotSelectionProbability(ConfigProperties properties) {
    String selectionProbabilityPropertyValue =
        properties.getString(
            SELECTION_PROBABILITY_KEY, String.valueOf(DEFAULT_SELECTION_PROBABILITY));
    try {
      double selectionProbability = Double.parseDouble(selectionProbabilityPropertyValue);
      if (selectionProbability > MAX_SELECTION_PROBABILITY) {
        logger.warning(
            "Configured snapshot selection probability of '"
                + selectionProbabilityPropertyValue
                + "' is higher than the maximum allowed probability. Using maximum allowed snapshot selection probability of '"
                + MAX_SELECTION_PROBABILITY
                + "'");
        return MAX_SELECTION_PROBABILITY;
      }
      return selectionProbability;
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

  static int getStackDepth(ConfigProperties properties) {
    return properties.getInt(STACK_DEPTH_KEY, DEFAULT_STACK_DEPTH);
  }

  static Duration getSamplingInterval(ConfigProperties properties) {
    return properties.getDuration(SAMPLING_INTERVAL_KEY, DEFAULT_SAMPLING_INTERVAL);
  }

  static Duration getExportInterval(ConfigProperties properties) {
    return properties.getDuration(EXPORT_INTERVAL_KEY, DEFAULT_EXPORT_INTERVAL);
  }

  static int getStagingCapacity(ConfigProperties properties) {
    return properties.getInt(STAGING_CAPACITY_KEY, DEFAULT_STAGING_CAPACITY);
  }

  private static void log(String key, Object value) {
    logger.fine(" " + pad(key) + " : " + value);
  }

  private static String pad(String str) {
    return String.format("%42s", str);
  }
}
