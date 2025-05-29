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

  private static final String PREFIX = "splunk.snapshot.profiler";
  public static final String CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER = PREFIX + ".enabled";

  private static final String SELECTION_RATE_KEY = "splunk.snapshot.selection.rate";
  private static final double DEFAULT_SELECTION_RATE = 0.01;
  private static final double MAX_SELECTION_RATE = 0.10;

  private static final String STACK_DEPTH_KEY = PREFIX + ".max.stack.depth";
  private static final int DEFAULT_STACK_DEPTH = 1024;

  private static final String SAMPLING_INTERVAL_KEY = PREFIX + ".sampling.interval";
  private static final String DEFAULT_SAMPLING_INTERVAL_STRING = "10ms";
  private static final Duration DEFAULT_SAMPLING_INTERVAL = Duration.ofMillis(10);

  private static final String EXPORT_INTERVAL_KEY = PREFIX + ".export.interval";
  private static final String DEFAULT_EXPORT_INTERVAL_STRING = "5s";
  private static final Duration DEFAULT_EXPORT_INTERVAL = Duration.ofSeconds(5);

  private static final String STAGING_CAPACITY_KEY = PREFIX + ".staging.capacity";
  private static final int DEFAULT_STAGING_CAPACITY = 2000;

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addPropertiesSupplier(
        () -> {
          Map<String, String> map = new HashMap<>();
          map.put(CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER, String.valueOf(false));
          map.put(SELECTION_RATE_KEY, String.valueOf(DEFAULT_SELECTION_RATE));
          map.put(STACK_DEPTH_KEY, String.valueOf(DEFAULT_STACK_DEPTH));
          map.put(SAMPLING_INTERVAL_KEY, DEFAULT_SAMPLING_INTERVAL_STRING);
          map.put(EXPORT_INTERVAL_KEY, DEFAULT_EXPORT_INTERVAL_STRING);
          map.put(STAGING_CAPACITY_KEY, String.valueOf(DEFAULT_STAGING_CAPACITY));
          return map;
        });
  }

  static boolean isSnapshotProfilingEnabled(ConfigProperties properties) {
    return properties.getBoolean(CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER, false);
  }

  static double getSnapshotSelectionRate(ConfigProperties properties) {
    String selectionRatePropertyValue =
        properties.getString(
            SELECTION_RATE_KEY, String.valueOf(DEFAULT_SELECTION_RATE));
    try {
      double selectionRate = Double.parseDouble(selectionRatePropertyValue);
      if (selectionRate > MAX_SELECTION_RATE) {
        logger.warning(
            "Configured snapshot selection rate of '"
                + selectionRatePropertyValue
                + "' is higher than the maximum allowed rate. Using maximum allowed snapshot selection rate of '"
                + MAX_SELECTION_RATE
                + "'");
        return MAX_SELECTION_RATE;
      }
      return selectionRate;
    } catch (NumberFormatException e) {
      logger.warning(
          "Invalid snapshot selection rate: '"
              + selectionRatePropertyValue
              + "', using default rate of '"
              + DEFAULT_SELECTION_RATE
              + "'");
      return DEFAULT_SELECTION_RATE;
    }
  }

  static int getSnapshotProfilerStackDepth(ConfigProperties properties) {
    return properties.getInt(STACK_DEPTH_KEY, DEFAULT_STACK_DEPTH);
  }

  static Duration getSnapshotProfilerSamplingInterval(ConfigProperties properties) {
    return properties.getDuration(SAMPLING_INTERVAL_KEY, DEFAULT_SAMPLING_INTERVAL);
  }

  static Duration getSnapshotProfilerExportInterval(ConfigProperties properties) {
    return properties.getDuration(EXPORT_INTERVAL_KEY, DEFAULT_EXPORT_INTERVAL);
  }

  static int getSnapshotProfilerStagingCapacity(ConfigProperties properties) {
    return properties.getInt(STAGING_CAPACITY_KEY, DEFAULT_STAGING_CAPACITY);
  }

  static void log(ConfigProperties properties) {
    logger.fine("Snapshot Profiler Configuration:");
    logger.fine("-------------------------------------------------------");

    log(CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER, isSnapshotProfilingEnabled(properties));
    log(SELECTION_RATE_KEY, getSnapshotSelectionRate(properties));
    log(STACK_DEPTH_KEY, getSnapshotProfilerStackDepth(properties));
    log(SAMPLING_INTERVAL_KEY, getSnapshotProfilerSamplingInterval(properties));
    log(EXPORT_INTERVAL_KEY, getSnapshotProfilerExportInterval(properties));
    log(STAGING_CAPACITY_KEY, getSnapshotProfilerStagingCapacity(properties));
    logger.fine("-------------------------------------------------------");
  }

  private static void log(String key, Object value) {
    logger.fine(" " + pad(key) + " : " + value);
  }

  private static String pad(String str) {
    return String.format("%42s", str);
  }
}
