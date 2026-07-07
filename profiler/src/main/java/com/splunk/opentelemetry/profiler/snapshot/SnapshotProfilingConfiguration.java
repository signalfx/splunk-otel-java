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

import com.splunk.opentelemetry.profiler.util.OptionalConfigurableSupplier;
import java.time.Duration;
import java.util.Objects;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class SnapshotProfilingConfiguration {
  public static final OptionalConfigurableSupplier<SnapshotProfilingConfiguration> SUPPLIER =
      new OptionalConfigurableSupplier<>();

  private static final Logger logger =
      Logger.getLogger(SnapshotProfilingConfiguration.class.getName());

  static final double MAX_SELECTION_PROBABILITY = 1.0;
  static final double DEFAULT_SELECTION_PROBABILITY = 0.01;
  static final int DEFAULT_STACK_DEPTH = 1024;
  static final long DEFAULT_SAMPLING_INTERVAL = 10;
  static final long DEFAULT_EXPORT_INTERVAL = 5000;
  static final int DEFAULT_STAGING_CAPACITY = 2000;

  private final boolean enabled;
  private final double snapshotSelectionProbability;
  private final int stackDepth;
  private final Duration samplingInterval;
  private final Duration exportInterval;
  private final int stagingCapacity;
  @Nullable private final Object configProperties;

  private SnapshotProfilingConfiguration(Builder builder) {
    enabled = builder.enabled;
    snapshotSelectionProbability = builder.snapshotSelectionProbability;
    stackDepth = builder.stackDepth;
    samplingInterval = builder.samplingInterval;
    exportInterval = builder.exportInterval;
    stagingCapacity = builder.stagingCapacity;
    configProperties = builder.configProperties;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

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

  public boolean isEnabled() {
    return enabled;
  }

  public double getSnapshotSelectionProbability() {
    return snapshotSelectionProbability;
  }

  public int getStackDepth() {
    return stackDepth;
  }

  public Duration getSamplingInterval() {
    return samplingInterval;
  }

  public Duration getExportInterval() {
    return exportInterval;
  }

  public int getStagingCapacity() {
    return stagingCapacity;
  }

  @Nullable
  public Object getConfigProperties() {
    return configProperties;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof SnapshotProfilingConfiguration)) {
      return false;
    }
    SnapshotProfilingConfiguration that = (SnapshotProfilingConfiguration) other;
    return enabled == that.enabled
        && Double.compare(that.snapshotSelectionProbability, snapshotSelectionProbability) == 0
        && stackDepth == that.stackDepth
        && stagingCapacity == that.stagingCapacity
        && Objects.equals(samplingInterval, that.samplingInterval)
        && Objects.equals(exportInterval, that.exportInterval)
        && Objects.equals(configProperties, that.configProperties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        enabled,
        snapshotSelectionProbability,
        stackDepth,
        samplingInterval,
        exportInterval,
        stagingCapacity,
        configProperties);
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
          "Snapshot selection probability must be greater than 0. Using default snapshot "
              + "selection probability of '"
              + DEFAULT_SELECTION_PROBABILITY
              + "' instead.");
      return DEFAULT_SELECTION_PROBABILITY;
    }
    return selectionProbability;
  }

  private static void log(String key, Object value) {
    logger.info(String.format("%24s : %s", key, value));
  }

  public static class Builder {
    private boolean enabled;
    private double snapshotSelectionProbability = DEFAULT_SELECTION_PROBABILITY;
    private int stackDepth = DEFAULT_STACK_DEPTH;
    private Duration samplingInterval = Duration.ofMillis(DEFAULT_SAMPLING_INTERVAL);
    private Duration exportInterval = Duration.ofMillis(DEFAULT_EXPORT_INTERVAL);
    private int stagingCapacity = DEFAULT_STAGING_CAPACITY;
    @Nullable private Object configProperties;

    private Builder() {}

    private Builder(SnapshotProfilingConfiguration config) {
      enabled = config.enabled;
      snapshotSelectionProbability = config.snapshotSelectionProbability;
      stackDepth = config.stackDepth;
      samplingInterval = config.samplingInterval;
      exportInterval = config.exportInterval;
      stagingCapacity = config.stagingCapacity;
      configProperties = config.configProperties;
    }

    public SnapshotProfilingConfiguration build() {
      return new SnapshotProfilingConfiguration(this);
    }

    public Builder setEnabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public Builder setSnapshotSelectionProbability(double snapshotSelectionProbability) {
      this.snapshotSelectionProbability = snapshotSelectionProbability;
      return this;
    }

    public Builder setStackDepth(int stackDepth) {
      this.stackDepth = stackDepth;
      return this;
    }

    public Builder setSamplingInterval(Duration samplingInterval) {
      this.samplingInterval = Objects.requireNonNull(samplingInterval);
      return this;
    }

    public Builder setExportInterval(Duration exportInterval) {
      this.exportInterval = Objects.requireNonNull(exportInterval);
      return this;
    }

    public Builder setStagingCapacity(int stagingCapacity) {
      this.stagingCapacity = stagingCapacity;
      return this;
    }

    public Builder setConfigProperties(@Nullable Object configProperties) {
      this.configProperties = configProperties;
      return this;
    }
  }
}
