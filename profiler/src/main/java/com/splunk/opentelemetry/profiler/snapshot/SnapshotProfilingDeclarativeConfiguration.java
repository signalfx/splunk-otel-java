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

import com.splunk.opentelemetry.profiler.util.OptionalConfigurableSupplier;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import java.time.Duration;
import java.util.logging.Logger;

public class SnapshotProfilingDeclarativeConfiguration implements SnapshotProfilingConfiguration {
  public static final OptionalConfigurableSupplier<SnapshotProfilingDeclarativeConfiguration>
      SUPPLIER = new OptionalConfigurableSupplier<>();

  private static final Logger logger =
      Logger.getLogger(SnapshotProfilingDeclarativeConfiguration.class.getName());
  private final DeclarativeConfigProperties profilingConfig;

  private static final String ROOT_NODE_NAME = "callgraphs";

  SnapshotProfilingDeclarativeConfiguration(DeclarativeConfigProperties profilingConfig) {
    this.profilingConfig = profilingConfig;
  }

  @Override
  public void log() {
    logger.info("Snapshot Profiler Configuration:");
    logger.info("--------------------------------");

    log("Enabled", isEnabled());
    log("SelectionProbability", getSnapshotSelectionProbability());
    log("StackDepth", getStackDepth());
    log("SamplingInterval", getSamplingInterval().toMillis() + "ms");
    log("ExportInterval", getExportInterval().toMillis() + "ms");
    log("StagingCapacity", getStagingCapacity());

    logger.info("--------------------------------");
  }

  @Override
  public boolean isEnabled() {
    return (profilingConfig != null) && profilingConfig.getPropertyKeys().contains(ROOT_NODE_NAME);
  }

  @Override
  public double getSnapshotSelectionProbability() {
    double selectionProbability =
        getConfigRoot().getDouble("selection_probability", DEFAULT_SELECTION_PROBABILITY);
    return SnapshotProfilingConfiguration.validateSelectionProbability(
        selectionProbability, logger);
  }

  @Override
  public int getStackDepth() {
    return getConfigRoot().getInt("stack_depth", DEFAULT_STACK_DEPTH);
  }

  @Override
  public Duration getSamplingInterval() {
    return getDuration("sampling_interval", DEFAULT_SAMPLING_INTERVAL);
  }

  @Override
  public Duration getExportInterval() {
    return getDuration("export_interval", DEFAULT_EXPORT_INTERVAL);
  }

  @Override
  public int getStagingCapacity() {
    return getConfigRoot().getInt("staging_capacity", DEFAULT_STAGING_CAPACITY);
  }

  @Override
  public Object getConfigProperties() {
    return profilingConfig;
  }

  private DeclarativeConfigProperties getConfigRoot() {
    return profilingConfig.getStructured(ROOT_NODE_NAME, empty());
  }

  private void log(String key, Object value) {
    logger.info(String.format("%24s : %s", key, value));
  }

  private Duration getDuration(String key, long defaultValue) {
    return Duration.ofMillis(getConfigRoot().getLong(key, defaultValue));
  }
}
