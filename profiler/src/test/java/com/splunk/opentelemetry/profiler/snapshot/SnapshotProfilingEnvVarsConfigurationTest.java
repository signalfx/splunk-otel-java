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

class SnapshotProfilingEnvVarsConfigurationTest {

  private static final ComponentLoader COMPONENT_LOADER =
      ComponentLoader.forClassLoader(
          SnapshotProfilingEnvVarsConfigurationTest.class.getClassLoader());

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldSupportEnabledAndDisabledSnapshotProfiler(boolean enabled) {
    // given
    var properties =
        DefaultConfigProperties.create(
            Map.of(
                SnapshotProfilingEnvVarsConfiguration.CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER,
                String.valueOf(enabled)),
            COMPONENT_LOADER);
    var configuration = new SnapshotProfilingEnvVarsConfiguration(properties);

    // when
    boolean actual = configuration.isEnabled();

    // then
    assertThat(actual).isEqualTo(enabled);
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.10, 0.05, 0.00001})
  void shouldReturnValidSelectionProbability(double selectionRate) {
    // given
    var properties =
        DefaultConfigProperties.create(
            Map.of(
                SnapshotProfilingEnvVarsConfiguration.SELECTION_PROBABILITY_KEY,
                String.valueOf(selectionRate)),
            COMPONENT_LOADER);
    var configuration = new SnapshotProfilingEnvVarsConfiguration(properties);

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
                SnapshotProfilingEnvVarsConfiguration.SELECTION_PROBABILITY_KEY,
                String.valueOf(selectionRate)),
            COMPONENT_LOADER);
    var configuration = new SnapshotProfilingEnvVarsConfiguration(properties);

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
            Map.of(SnapshotProfilingEnvVarsConfiguration.SELECTION_PROBABILITY_KEY, "0"),
            COMPONENT_LOADER);
    var configuration = new SnapshotProfilingEnvVarsConfiguration(properties);

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
            Map.of(SnapshotProfilingEnvVarsConfiguration.SELECTION_PROBABILITY_KEY, "not-a-number"),
            COMPONENT_LOADER);
    var configuration = new SnapshotProfilingEnvVarsConfiguration(properties);

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
            Map.of(SnapshotProfilingEnvVarsConfiguration.STACK_DEPTH_KEY, String.valueOf(depth)),
            COMPONENT_LOADER);
    var configuration = new SnapshotProfilingEnvVarsConfiguration(properties);

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
                SnapshotProfilingEnvVarsConfiguration.SAMPLING_INTERVAL_KEY,
                samplingInterval + "ms"),
            COMPONENT_LOADER);
    var configuration = new SnapshotProfilingEnvVarsConfiguration(properties);

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
                SnapshotProfilingEnvVarsConfiguration.EXPORT_INTERVAL_KEY, exportInterval + "ms"),
            COMPONENT_LOADER);
    var configuration = new SnapshotProfilingEnvVarsConfiguration(properties);

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
                SnapshotProfilingEnvVarsConfiguration.STAGING_CAPACITY_KEY,
                String.valueOf(capacity)),
            COMPONENT_LOADER);
    var configuration = new SnapshotProfilingEnvVarsConfiguration(properties);

    // when
    int actual = configuration.getStagingCapacity();

    // then
    assertThat(actual).isEqualTo(capacity);
  }
  /*
  @Nested
  class DeclarativeConfigTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void isSnapshotProfilingEnabled(boolean enabled) {
      // given
      DeclarativeConfigPropertiesBridgeBuilder builder =
          new DeclarativeConfigPropertiesBridgeBuilder();
      builder.addOverride(
          SnapshotProfilingEnvVarsConfiguration.CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER, enabled);

      // when
      var properties = builder.buildFromInstrumentationConfig(null);

      // then
      assertThat(new SnapshotProfilingEnvVarsConfiguration(properties).isSnapshotProfilingEnabled())
          .isEqualTo(enabled);
    }

    @ParameterizedTest
    @ValueSource(ints = {128, 512, 2056})
    void getConfiguredSnapshotProfilerStackDepth(int depth) {
      // given
      DeclarativeConfigPropertiesBridgeBuilder builder =
          new DeclarativeConfigPropertiesBridgeBuilder();
      builder.addOverride(SnapshotProfilingEnvVarsConfiguration.STACK_DEPTH_KEY, depth);

      // when
      var properties = builder.buildFromInstrumentationConfig(null);

      // then
      assertThat(new SnapshotProfilingEnvVarsConfiguration(properties).getStackDepth())
          .isEqualTo(depth);
    }

    @ParameterizedTest
    @ValueSource(longs = {128, 512, 2056})
    void getConfiguredSnapshotProfilerSamplingInterval(long milliseconds) {
      // given
      DeclarativeConfigPropertiesBridgeBuilder builder =
          new DeclarativeConfigPropertiesBridgeBuilder();
      builder.addOverride(
          SnapshotProfilingEnvVarsConfiguration.SAMPLING_INTERVAL_KEY, milliseconds);

      // when
      var properties = builder.buildFromInstrumentationConfig(null);

      // then
      assertThat(new SnapshotProfilingEnvVarsConfiguration(properties).getSamplingInterval())
          .isEqualTo(Duration.ofMillis(milliseconds));
    }

    @ParameterizedTest
    @ValueSource(longs = {128, 512, 2056})
    void getConfiguredSnapshotProfilerEmptyStagingInterval(long milliseconds) {
      // given
      DeclarativeConfigPropertiesBridgeBuilder builder =
          new DeclarativeConfigPropertiesBridgeBuilder();
      builder.addOverride(SnapshotProfilingEnvVarsConfiguration.EXPORT_INTERVAL_KEY, milliseconds);

      // when
      var properties = builder.buildFromInstrumentationConfig(null);

      // then
      assertThat(new SnapshotProfilingEnvVarsConfiguration(properties).getExportInterval())
          .isEqualTo(Duration.ofMillis(milliseconds));
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 1000, 10_000})
    void getConfiguredSnapshotProfilerStagingCapacity(int value) {
      // given
      DeclarativeConfigPropertiesBridgeBuilder builder =
          new DeclarativeConfigPropertiesBridgeBuilder();
      builder.addOverride(SnapshotProfilingEnvVarsConfiguration.STAGING_CAPACITY_KEY, value);

      // when
      var properties = builder.buildFromInstrumentationConfig(null);

      // then
      assertThat(new SnapshotProfilingEnvVarsConfiguration(properties).getStagingCapacity())
          .isEqualTo(value);
    }
  }

  @Nested
  class LoggingTest {
    @RegisterExtension
    private final LogCapturer log =
        LogCapturer.create()
            .captureForType(SnapshotProfilingEnvVarsConfiguration.class, Level.DEBUG);

    @Test
    void includeSnapshotProfilingHeading() {
      // given
      var properties = DefaultConfigProperties.create(Collections.emptyMap(), COMPONENT_LOADER);

      // when
      new SnapshotProfilingEnvVarsConfiguration(properties).log();

      // then
      log.assertContains("-----------------------");
      log.assertContains("Snapshot Profiler Configuration:");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void includeSnapshotProfilingEnabled(boolean enabled) {
      // given
      var properties =
          DefaultConfigProperties.create(
              Map.of(
                  SnapshotProfilingEnvVarsConfiguration.CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER,
                  String.valueOf(enabled)),
              COMPONENT_LOADER);
      // when
      new SnapshotProfilingEnvVarsConfiguration(properties).log();
      // then
      log.assertContains(
          SnapshotProfilingEnvVarsConfiguration.CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER
              + " : "
              + enabled);
    }

    @ParameterizedTest
    @ValueSource(doubles = {.01, .05, .1})
    void includeSnapshotProfilingSelectionRate(double rate) {
      // given
      var properties =
          DefaultConfigProperties.create(
              Map.of(
                  SnapshotProfilingEnvVarsConfiguration.SELECTION_PROBABILITY_KEY,
                  String.valueOf(rate)),
              COMPONENT_LOADER);
      // when
      new SnapshotProfilingEnvVarsConfiguration(properties).log();
      // then
      log.assertContains(
          SnapshotProfilingEnvVarsConfiguration.SELECTION_PROBABILITY_KEY + " : " + rate);
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 1000, 10_1000})
    void includeSnapshotProfilingStackTraceDepth(int depth) {
      // given
      var properties =
          DefaultConfigProperties.create(
              Map.of(SnapshotProfilingEnvVarsConfiguration.STACK_DEPTH_KEY, String.valueOf(depth)),
              COMPONENT_LOADER);
      // when
      new SnapshotProfilingEnvVarsConfiguration(properties).log();
      // then
      log.assertContains(SnapshotProfilingEnvVarsConfiguration.STACK_DEPTH_KEY + " : " + depth);
    }

    @ParameterizedTest
    @ValueSource(strings = {"5ms", "10ms", "100ms"})
    void includeSnapshotProfilingSamplingInterval(String interval) {
      // given
      var properties =
          DefaultConfigProperties.create(
              Map.of(SnapshotProfilingEnvVarsConfiguration.SAMPLING_INTERVAL_KEY, interval),
              COMPONENT_LOADER);

      // when
      new SnapshotProfilingEnvVarsConfiguration(properties).log();

      // then
      var duration =
          properties.getDuration(SnapshotProfilingEnvVarsConfiguration.SAMPLING_INTERVAL_KEY);
      log.assertContains(
          SnapshotProfilingEnvVarsConfiguration.SAMPLING_INTERVAL_KEY + " : " + duration);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10s", "30s", "10m"})
    void includeSnapshotProfilingExportInterval(String interval) {
      // given
      var properties =
          DefaultConfigProperties.create(
              Map.of(SnapshotProfilingEnvVarsConfiguration.EXPORT_INTERVAL_KEY, interval),
              COMPONENT_LOADER);

      // when
      new SnapshotProfilingEnvVarsConfiguration(properties).log();

      // then
      var duration =
          properties.getDuration(SnapshotProfilingEnvVarsConfiguration.EXPORT_INTERVAL_KEY);
      log.assertContains(
          SnapshotProfilingEnvVarsConfiguration.EXPORT_INTERVAL_KEY + " : " + duration);
    }

    @ParameterizedTest
    @ValueSource(ints = {1000, 2000, 10_1000})
    void includeSnapshotProfilingStagingCapacity(int capacity) {
      // given
      var properties =
          DefaultConfigProperties.create(
              Map.of(
                  SnapshotProfilingEnvVarsConfiguration.STAGING_CAPACITY_KEY,
                  String.valueOf(capacity)),
              COMPONENT_LOADER);
      // when
      new SnapshotProfilingEnvVarsConfiguration(properties).log();
      // then
      log.assertContains(
          SnapshotProfilingEnvVarsConfiguration.STAGING_CAPACITY_KEY + " : " + capacity);
    }
  }*/
}
