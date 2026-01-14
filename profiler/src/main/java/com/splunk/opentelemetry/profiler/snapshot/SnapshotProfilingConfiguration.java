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
import io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.time.Duration;
import java.util.Optional;
import java.util.logging.Logger;

public interface SnapshotProfilingConfiguration {
  double MAX_SELECTION_PROBABILITY = 1.0;
  double DEFAULT_SELECTION_PROBABILITY = 0.01;
  int DEFAULT_STACK_DEPTH = 1024;
  long DEFAULT_SAMPLING_INTERVAL = 10;
  long DEFAULT_EXPORT_INTERVAL = 5000;
  int DEFAULT_STAGING_CAPACITY = 2000;

  void log();

  boolean isEnabled();

  double getSnapshotSelectionProbability();

  int getStackDepth();

  Duration getSamplingInterval();

  Duration getExportInterval();

  int getStagingCapacity();

  Object getConfigProperties();

  static SnapshotProfilingConfiguration fromSdk(AutoConfiguredOpenTelemetrySdk sdk) {
    if (AutoConfigureUtil.isDeclarativeConfig(sdk)) {
      DeclarativeConfigProperties distributionConfig = AutoConfigureUtil.getDistributionConfig(sdk);
      distributionConfig = Optional.ofNullable(distributionConfig).orElse(empty());
      return new SnapshotProfilingDeclarativeConfiguration(
          distributionConfig.getStructured("splunk", empty()).getStructured("profiling", empty()));
    } else {
      ConfigProperties configProperties = AutoConfigureUtil.getConfig(sdk);
      return new SnapshotProfilingEnvVarsConfiguration(configProperties);
    }
  }

  static double validateSelectionProbability(double selectionProbability, Logger logger) {
    if (selectionProbability > MAX_SELECTION_PROBABILITY) {
      logger.warning(
          "Configured snapshot selection probability of '"
              + selectionProbability
              + "' is higher than the maximum allowed probability. Using maximum allowed snapshot selection probability of '"
              + MAX_SELECTION_PROBABILITY
              + "'");
      return MAX_SELECTION_PROBABILITY;
    }
    if (selectionProbability <= 0) {
      logger.warning(
          "Snapshot selection probability must be greater than 0. Using default snapshot"
              + "selection probability of '"
              + DEFAULT_SELECTION_PROBABILITY
              + "' instead.");
      return DEFAULT_SELECTION_PROBABILITY;
    }
    return selectionProbability;
  }
}
