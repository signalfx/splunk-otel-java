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

import static io.opentelemetry.api.incubator.config.DeclarativeConfigProperties.empty;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import java.time.Duration;
import java.util.logging.Logger;

public final class SnapshotProfilingDeclarativeConfigurationFactory {
  private static final Logger logger =
      Logger.getLogger(SnapshotProfilingDeclarativeConfigurationFactory.class.getName());

  private static final String ROOT_NODE_NAME = "callgraphs";

  private SnapshotProfilingDeclarativeConfigurationFactory() {}

  public static SnapshotProfilingConfiguration create(DeclarativeConfigProperties profilingConfig) {
    DeclarativeConfigProperties config = profilingConfig == null ? empty() : profilingConfig;
    DeclarativeConfigProperties configRoot = getConfigRoot(config);

    return SnapshotProfilingConfiguration.builder()
        .setEnabled(config.getPropertyKeys().contains(ROOT_NODE_NAME))
        .setSnapshotSelectionProbability(getSnapshotSelectionProbability(configRoot))
        .setStackDepth(
            configRoot.getInt("stack_depth", SnapshotProfilingConfiguration.DEFAULT_STACK_DEPTH))
        .setSamplingInterval(
            getDuration(
                configRoot,
                "sampling_interval",
                SnapshotProfilingConfiguration.DEFAULT_SAMPLING_INTERVAL))
        .setExportInterval(
            getDuration(
                configRoot,
                "export_interval",
                SnapshotProfilingConfiguration.DEFAULT_EXPORT_INTERVAL))
        .setStagingCapacity(
            configRoot.getInt(
                "staging_capacity", SnapshotProfilingConfiguration.DEFAULT_STAGING_CAPACITY))
        .setConfigProperties(config)
        .build();
  }

  private static DeclarativeConfigProperties getConfigRoot(DeclarativeConfigProperties config) {
    return config.getStructured(ROOT_NODE_NAME, empty());
  }

  private static double getSnapshotSelectionProbability(DeclarativeConfigProperties configRoot) {
    double selectionProbability =
        configRoot.getDouble(
            "selection_probability", SnapshotProfilingConfiguration.DEFAULT_SELECTION_PROBABILITY);
    return SnapshotProfilingConfiguration.validateSelectionProbability(
        selectionProbability, logger);
  }

  private static Duration getDuration(
      DeclarativeConfigProperties config, String key, long defaultValue) {
    return Duration.ofMillis(config.getLong(key, defaultValue));
  }
}
