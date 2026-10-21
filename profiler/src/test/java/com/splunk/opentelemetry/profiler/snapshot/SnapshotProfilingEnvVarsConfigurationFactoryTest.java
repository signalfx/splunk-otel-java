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

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SnapshotProfilingEnvVarsConfigurationFactoryTest {

  private static final ComponentLoader COMPONENT_LOADER =
      ComponentLoader.forClassLoader(
          SnapshotProfilingEnvVarsConfigurationFactoryTest.class.getClassLoader());

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldSupportEnabledAndDisabledSnapshotProfiler(boolean enabled) {
    // given
    var properties =
        DefaultConfigProperties.create(
            Map.of(
                SnapshotProfilingEnvVarsConfigurationFactory.CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER,
                String.valueOf(enabled)),
            COMPONENT_LOADER);
    var configuration = SnapshotProfilingEnvVarsConfigurationFactory.create(properties);

    // when
    boolean actual = configuration.isEnabled();

    // then
    assertThat(actual).isEqualTo(enabled);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldMapLocksEnabled(boolean enabled) {
    var properties =
        DefaultConfigProperties.create(
            Map.of(
                SnapshotProfilingEnvVarsConfigurationFactory.LOCKS_ENABLED_KEY,
                String.valueOf(enabled)),
            COMPONENT_LOADER);

    var configuration = SnapshotProfilingEnvVarsConfigurationFactory.create(properties);

    assertThat(configuration.getLocksEnabled()).isEqualTo(enabled);
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.10, 0.05, 0.00001})
  void shouldReturnValidSelectionProbability(double selectionRate) {
    // given
    var properties =
        DefaultConfigProperties.create(
            Map.of(
                SnapshotProfilingEnvVarsConfigurationFactory.SELECTION_PROBABILITY_KEY,
                String.valueOf(selectionRate)),
            COMPONENT_LOADER);
    var configuration = SnapshotProfilingEnvVarsConfigurationFactory.create(properties);

    // when
    double actualSelectionRate = configuration.getSnapshotSelectionProbability();

    // then
    assertThat(actualSelectionRate).isEqualTo(selectionRate);
  }

  @ParameterizedTest
  @ValueSource(doubles = {1.1, 5.0, 1.9, 1.0})
  void shouldReturnMaxSelectionProbabilityWhenAboveMax(double selectionRate) {
    // given
    var properties =
        DefaultConfigProperties.create(
            Map.of(
                SnapshotProfilingEnvVarsConfigurationFactory.SELECTION_PROBABILITY_KEY,
                String.valueOf(selectionRate)),
            COMPONENT_LOADER);
    var configuration = SnapshotProfilingEnvVarsConfigurationFactory.create(properties);

    // when
    double actualSelectionRate = configuration.getSnapshotSelectionProbability();

    // then
    assertThat(actualSelectionRate).isEqualTo(1.0);
  }

  @Test
  void shouldFallbackSelectionProbabilityToDefaultWhenZero() {
    // given
    var properties =
        DefaultConfigProperties.create(
            Map.of(SnapshotProfilingEnvVarsConfigurationFactory.SELECTION_PROBABILITY_KEY, "0"),
            COMPONENT_LOADER);
    var configuration = SnapshotProfilingEnvVarsConfigurationFactory.create(properties);

    // when
    double actualSelectionRate = configuration.getSnapshotSelectionProbability();

    // then
    assertThat(actualSelectionRate).isEqualTo(0.01);
  }

  @Test
  void shouldReturnDefaultSelectionProbabilityWhenNotANumber() {
    // given
    var properties =
        DefaultConfigProperties.create(
            Map.of(
                SnapshotProfilingEnvVarsConfigurationFactory.SELECTION_PROBABILITY_KEY,
                "not-a-number"),
            COMPONENT_LOADER);
    var configuration = SnapshotProfilingEnvVarsConfigurationFactory.create(properties);

    // when
    double actualSelectionRate = configuration.getSnapshotSelectionProbability();

    // then
    assertThat(actualSelectionRate).isEqualTo(0.01);
  }

  @Test
  void shouldReturnStackDepth() {
    // given
    int depth = 123;
    var properties =
        DefaultConfigProperties.create(
            Map.of(
                SnapshotProfilingEnvVarsConfigurationFactory.STACK_DEPTH_KEY,
                String.valueOf(depth)),
            COMPONENT_LOADER);
    var configuration = SnapshotProfilingEnvVarsConfigurationFactory.create(properties);

    // when
    int actualDepth = configuration.getStackDepth();

    // then
    assertThat(actualDepth).isEqualTo(depth);
  }

  @Test
  void shouldReturnSamplingInterval() {
    // given
    long samplingInterval = 123456;
    var properties =
        DefaultConfigProperties.create(
            Map.of(
                SnapshotProfilingEnvVarsConfigurationFactory.SAMPLING_INTERVAL_KEY,
                samplingInterval + "ms"),
            COMPONENT_LOADER);
    var configuration = SnapshotProfilingEnvVarsConfigurationFactory.create(properties);

    // when
    Duration actual = configuration.getSamplingInterval();

    // then
    assertThat(actual).isEqualTo(Duration.ofMillis(samplingInterval));
  }

  @Test
  void shouldReturnExportInterval() {
    // given
    long exportInterval = 98765;
    var properties =
        DefaultConfigProperties.create(
            Map.of(
                SnapshotProfilingEnvVarsConfigurationFactory.EXPORT_INTERVAL_KEY,
                exportInterval + "ms"),
            COMPONENT_LOADER);
    var configuration = SnapshotProfilingEnvVarsConfigurationFactory.create(properties);

    // when
    Duration actual = configuration.getExportInterval();

    // then
    assertThat(actual).isEqualTo(Duration.ofMillis(exportInterval));
  }

  @Test
  void shouldReturnStagingCapacity() {
    // given
    int capacity = 123;
    var properties =
        DefaultConfigProperties.create(
            Map.of(
                SnapshotProfilingEnvVarsConfigurationFactory.STAGING_CAPACITY_KEY,
                String.valueOf(capacity)),
            COMPONENT_LOADER);
    var configuration = SnapshotProfilingEnvVarsConfigurationFactory.create(properties);

    // when
    int actual = configuration.getStagingCapacity();

    // then
    assertThat(actual).isEqualTo(capacity);
  }
}
