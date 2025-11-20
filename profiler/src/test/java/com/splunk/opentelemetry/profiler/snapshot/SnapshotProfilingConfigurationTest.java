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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.netmikey.logunit.api.LogCapturer;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.instrumentation.config.bridge.DeclarativeConfigPropertiesBridgeBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.event.Level;

class SnapshotProfilingConfigurationTest {

  private static final ComponentLoader COMPONENT_LOADER =
      ComponentLoader.forClassLoader(SnapshotProfilingConfigurationTest.class.getClassLoader());

  @Test
  void verifyDefaultValues() {
    var properties = DefaultConfigProperties.create(Collections.emptyMap(), COMPONENT_LOADER);

    assertThat(SnapshotProfilingConfiguration.isSnapshotProfilingEnabled(properties)).isFalse();
    assertThat(SnapshotProfilingConfiguration.getSnapshotSelectionProbability(properties))
        .isEqualTo(0.01);
    assertThat(SnapshotProfilingConfiguration.getStackDepth(properties)).isEqualTo(1024);
    assertThat(SnapshotProfilingConfiguration.getSamplingInterval(properties))
        .isEqualTo(Duration.ofMillis(10));
    assertThat(SnapshotProfilingConfiguration.getExportInterval(properties))
        .isEqualTo(Duration.ofSeconds(5));
    assertThat(SnapshotProfilingConfiguration.getStagingCapacity(properties)).isEqualTo(2000);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void isSnapshotProfilingEnabled(boolean enabled) {
    var properties =
        DefaultConfigProperties.create(
            Map.of("splunk.snapshot.profiler.enabled", String.valueOf(enabled)), COMPONENT_LOADER);
    assertEquals(enabled, SnapshotProfilingConfiguration.isSnapshotProfilingEnabled(properties));
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.10, 0.05, 0.0})
  void getSnapshotSelectionProbability(double selectionRate) {
    var properties =
        DefaultConfigProperties.create(
            Map.of("splunk.snapshot.selection.probability", String.valueOf(selectionRate)),
            COMPONENT_LOADER);

    double actualSelectionRate =
        SnapshotProfilingConfiguration.getSnapshotSelectionProbability(properties);
    assertEquals(selectionRate, actualSelectionRate);
  }

  @Test
  void getSnapshotSelectionProbabilityUsesDefaultValueIsNotNumeric() {
    var properties =
        DefaultConfigProperties.create(
            Map.of("splunk.snapshot.selection.probability", "not-a-number"), COMPONENT_LOADER);

    double actualSelectionRate =
        SnapshotProfilingConfiguration.getSnapshotSelectionProbability(properties);
    assertEquals(0.01, actualSelectionRate);
  }

  @ParameterizedTest
  @ValueSource(doubles = {1.0, 0.5, 0.11})
  void getSnapshotSelectionRateUsesMaxSelectionRateWhenConfiguredProbabilityIsHigher(
      double selectionRate) {
    var properties =
        DefaultConfigProperties.create(
            Map.of("splunk.snapshot.selection.probability", String.valueOf(selectionRate)),
            COMPONENT_LOADER);

    double actualSelectionRate =
        SnapshotProfilingConfiguration.getSnapshotSelectionProbability(properties);
    assertEquals(0.10, actualSelectionRate);
  }

  @ParameterizedTest
  @ValueSource(ints = {128, 512, 2056})
  void getConfiguredSnapshotProfilerStackDepth(int depth) {
    var properties =
        DefaultConfigProperties.create(
            Map.of("splunk.snapshot.profiler.max.stack.depth", String.valueOf(depth)),
            COMPONENT_LOADER);
    assertEquals(depth, SnapshotProfilingConfiguration.getStackDepth(properties));
  }

  @ParameterizedTest
  @ValueSource(longs = {128, 512, 2056})
  void getConfiguredSnapshotProfilerSamplingInterval(long milliseconds) {
    var properties =
        DefaultConfigProperties.create(
            Map.of("splunk.snapshot.sampling.interval", String.valueOf(milliseconds)),
            COMPONENT_LOADER);
    assertEquals(
        Duration.ofMillis(milliseconds),
        SnapshotProfilingConfiguration.getSamplingInterval(properties));
  }

  @ParameterizedTest
  @ValueSource(longs = {128, 512, 2056})
  void getConfiguredSnapshotProfilerEmptyStagingInterval(long milliseconds) {
    var properties =
        DefaultConfigProperties.create(
            Map.of("splunk.snapshot.profiler.export.interval", String.valueOf(milliseconds)),
            COMPONENT_LOADER);
    assertEquals(
        Duration.ofMillis(milliseconds),
        SnapshotProfilingConfiguration.getExportInterval(properties));
  }

  @ParameterizedTest
  @ValueSource(ints = {100, 1000, 10_000})
  void getConfiguredSnapshotProfilerStagingCapacity(int value) {
    var properties =
        DefaultConfigProperties.create(
            Map.of("splunk.snapshot.profiler.staging.capacity", String.valueOf(value)),
            COMPONENT_LOADER);
    assertEquals(value, SnapshotProfilingConfiguration.getStagingCapacity(properties));
  }

  @Nested
  class DeclarativeConfigTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void isSnapshotProfilingEnabled(boolean enabled) {
      DeclarativeConfigPropertiesBridgeBuilder builder =
          new DeclarativeConfigPropertiesBridgeBuilder();
      builder.addOverride("splunk.snapshot.profiler.enabled", enabled);

      var properties = builder.buildFromInstrumentationConfig(null);

      assertEquals(enabled, SnapshotProfilingConfiguration.isSnapshotProfilingEnabled(properties));
    }

    @ParameterizedTest
    @ValueSource(ints = {128, 512, 2056})
    void getConfiguredSnapshotProfilerStackDepth(int depth) {
      DeclarativeConfigPropertiesBridgeBuilder builder =
          new DeclarativeConfigPropertiesBridgeBuilder();
      builder.addOverride("splunk.snapshot.profiler.max.stack.depth", depth);

      var properties = builder.buildFromInstrumentationConfig(null);

      assertEquals(depth, SnapshotProfilingConfiguration.getStackDepth(properties));
    }

    @ParameterizedTest
    @ValueSource(longs = {128, 512, 2056})
    void getConfiguredSnapshotProfilerSamplingInterval(long milliseconds) {
      DeclarativeConfigPropertiesBridgeBuilder builder =
          new DeclarativeConfigPropertiesBridgeBuilder();
      builder.addOverride("splunk.snapshot.sampling.interval", milliseconds);

      var properties = builder.buildFromInstrumentationConfig(null);

      assertEquals(
          Duration.ofMillis(milliseconds),
          SnapshotProfilingConfiguration.getSamplingInterval(properties));
    }

    @ParameterizedTest
    @ValueSource(longs = {128, 512, 2056})
    void getConfiguredSnapshotProfilerEmptyStagingInterval(long milliseconds) {
      DeclarativeConfigPropertiesBridgeBuilder builder =
          new DeclarativeConfigPropertiesBridgeBuilder();
      builder.addOverride("splunk.snapshot.profiler.export.interval", milliseconds);

      var properties = builder.buildFromInstrumentationConfig(null);

      assertEquals(
          Duration.ofMillis(milliseconds),
          SnapshotProfilingConfiguration.getExportInterval(properties));
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 1000, 10_000})
    void getConfiguredSnapshotProfilerStagingCapacity(int value) {
      DeclarativeConfigPropertiesBridgeBuilder builder =
          new DeclarativeConfigPropertiesBridgeBuilder();
      builder.addOverride("splunk.snapshot.profiler.staging.capacity", value);

      var properties = builder.buildFromInstrumentationConfig(null);

      assertEquals(value, SnapshotProfilingConfiguration.getStagingCapacity(properties));
    }
  }

  @Nested
  class LoggingTest {
    @RegisterExtension
    private final LogCapturer log =
        LogCapturer.create().captureForType(SnapshotProfilingConfiguration.class, Level.DEBUG);

    @Test
    void includeSnapshotProfilingHeading() {
      var properties = DefaultConfigProperties.create(Collections.emptyMap(), COMPONENT_LOADER);

      SnapshotProfilingConfiguration.log(properties);

      log.assertContains("-----------------------");
      log.assertContains("Snapshot Profiler Configuration:");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void includeSnapshotProfilingEnabled(boolean enabled) {
      var properties =
          DefaultConfigProperties.create(
              Map.of("splunk.snapshot.profiler.enabled", String.valueOf(enabled)),
              COMPONENT_LOADER);
      SnapshotProfilingConfiguration.log(properties);
      log.assertContains("splunk.snapshot.profiler.enabled" + " : " + enabled);
    }

    @ParameterizedTest
    @ValueSource(doubles = {.01, .05, .1})
    void includeSnapshotProfilingSelectionRate(double rate) {
      var properties =
          DefaultConfigProperties.create(
              Map.of("splunk.snapshot.selection.probability", String.valueOf(rate)),
              COMPONENT_LOADER);
      SnapshotProfilingConfiguration.log(properties);
      log.assertContains("splunk.snapshot.selection.probability" + " : " + rate);
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 1000, 10_1000})
    void includeSnapshotProfilingStackTraceDepth(int depth) {
      var properties =
          DefaultConfigProperties.create(
              Map.of("splunk.snapshot.profiler.max.stack.depth", String.valueOf(depth)),
              COMPONENT_LOADER);
      SnapshotProfilingConfiguration.log(properties);
      log.assertContains("splunk.snapshot.profiler.max.stack.depth" + " : " + depth);
    }

    @ParameterizedTest
    @ValueSource(strings = {"5ms", "10ms", "100ms"})
    void includeSnapshotProfilingSamplingInterval(String interval) {
      var properties =
          DefaultConfigProperties.create(
              Map.of("splunk.snapshot.sampling.interval", interval), COMPONENT_LOADER);

      SnapshotProfilingConfiguration.log(properties);

      var duration = properties.getDuration("splunk.snapshot.sampling.interval");
      log.assertContains("splunk.snapshot.sampling.interval" + " : " + duration);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10s", "30s", "10m"})
    void includeSnapshotProfilingExportInterval(String interval) {
      var properties =
          DefaultConfigProperties.create(
              Map.of("splunk.snapshot.profiler.export.interval", interval), COMPONENT_LOADER);

      SnapshotProfilingConfiguration.log(properties);

      var duration = properties.getDuration("splunk.snapshot.profiler.export.interval");
      log.assertContains("splunk.snapshot.profiler.export.interval" + " : " + duration);
    }

    @ParameterizedTest
    @ValueSource(ints = {1000, 2000, 10_1000})
    void includeSnapshotProfilingStagingCapacity(int capacity) {
      var properties =
          DefaultConfigProperties.create(
              Map.of("splunk.snapshot.profiler.staging.capacity", String.valueOf(capacity)),
              COMPONENT_LOADER);
      SnapshotProfilingConfiguration.log(properties);
      log.assertContains("splunk.snapshot.profiler.staging.capacity" + " : " + capacity);
    }
  }
}
