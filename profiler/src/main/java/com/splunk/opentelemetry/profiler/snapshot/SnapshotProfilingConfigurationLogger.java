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
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.logging.Logger;

class SnapshotProfilingConfigurationLogger {
  private static final Logger logger =
      Logger.getLogger(SnapshotProfilingConfigurationLogger.class.getName());

  static void log(ConfigProperties properties) {
    logger.info("Snapshot Profiler Configuration:");
    logger.info("-------------------------------------------------------");
    // 32

    log(
        Configuration.CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER,
        Configuration.isSnapshotProfilingEnabled(properties));
    log(
        Configuration.CONFIG_KEY_SNAPSHOT_SELECTION_RATE,
        Configuration.getSnapshotSelectionRate(properties));
    log(
        Configuration.CONFIG_KEY_SNAPSHOT_PROFILER_STACK_DEPTH,
        Configuration.getSnapshotProfilerStackDepth(properties));
    log(
        Configuration.CONFIG_KEY_SNAPSHOT_PROFILER_SAMPLING_INTERVAL,
        Configuration.getSnapshotProfilerSamplingInterval(properties));
    log(
        Configuration.CONFIG_KEY_SNAPSHOT_PROFILER_EXPORT_INTERVAL,
        Configuration.getSnapshotProfilerExportInterval(properties));
    log(
        Configuration.CONFIG_KEY_SNAPSHOT_PROFILER_STAGING_CAPACITY,
        Configuration.getSnapshotProfilerStagingCapacity(properties));
    logger.info("-------------------------------------------------------");
  }

  private static void log(String key, Object value) {
    logger.info(" " + pad(key) + " : " + value);
  }

  private static String pad(String str) {
    return String.format("%42s", str);
  }
}
