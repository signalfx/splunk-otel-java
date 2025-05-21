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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.netmikey.logunit.api.LogCapturer;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkExtension;
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
  @Nested
  class DefaultValuesTest {
    private final SnapshotProfilingConfiguration configuration =
        new SnapshotProfilingConfiguration();

    @RegisterExtension
    private final OpenTelemetrySdkExtension sdk =
        OpenTelemetrySdkExtension.configure().with(configuration).build();

    @Test
    void defaultValuesAreProvidedToOpenTelemetry() {
      var properties = sdk.getConfig();
      assertEquals("false", properties.getString("splunk.snapshot.profiler.enabled"));
      assertEquals("0.01", properties.getString("splunk.snapshot.selection.rate"));
      assertEquals("1024", properties.getString("splunk.snapshot.profiler.max.stack.depth"));
      assertEquals("10ms", properties.getString("splunk.snapshot.profiler.sampling.interval"));
      assertEquals("5s", properties.getString("splunk.snapshot.profiler.export.interval"));
      assertEquals("2000", properties.getString("splunk.snapshot.profiler.staging.capacity"));
    }
  }

  @Test
  void isSnapshotProfilingEnabledIsFalseByDefault() {
    var properties = DefaultConfigProperties.create(Collections.emptyMap());
    assertFalse(SnapshotProfilingConfiguration.isSnapshotProfilingEnabled(properties));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void isSnapshotProfilingEnabled(boolean enabled) {
    var properties =
        DefaultConfigProperties.create(
            Map.of("splunk.snapshot.profiler.enabled", String.valueOf(enabled)));
    assertEquals(enabled, SnapshotProfilingConfiguration.isSnapshotProfilingEnabled(properties));
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.10, 0.05, 0.0})
  void getSnapshotSelectionRate(double selectionRate) {
    var properties =
        DefaultConfigProperties.create(
            Map.of("splunk.snapshot.selection.rate", String.valueOf(selectionRate)));

    double actualSelectionRate =
        SnapshotProfilingConfiguration.getSnapshotSelectionRate(properties);
    assertEquals(selectionRate, actualSelectionRate);
  }

  @Test
  void getSnapshotSelectionRateUsesDefaultWhenPropertyNotSet() {
    var properties = DefaultConfigProperties.create(Collections.emptyMap());

    double actualSelectionRate =
        SnapshotProfilingConfiguration.getSnapshotSelectionRate(properties);
    assertEquals(0.01, actualSelectionRate);
  }

  @Test
  void getSnapshotSelectionRateUsesDefaultValueIsNotNumeric() {
    var properties =
        DefaultConfigProperties.create(Map.of("splunk.snapshot.selection.rate", "not-a-number"));

    double actualSelectionRate =
        SnapshotProfilingConfiguration.getSnapshotSelectionRate(properties);
    assertEquals(0.01, actualSelectionRate);
  }

  @ParameterizedTest
  @ValueSource(doubles = {1.0, 0.5, 0.11})
  void getSnapshotSelectionRateUsesMaxSelectionRateWhenConfiguredRateIsHigher(
      double selectionRate) {
    var properties =
        DefaultConfigProperties.create(
            Map.of("splunk.snapshot.selection.rate", String.valueOf(selectionRate)));

    double actualSelectionRate =
        SnapshotProfilingConfiguration.getSnapshotSelectionRate(properties);
    assertEquals(0.10, actualSelectionRate);
  }

  @ParameterizedTest
  @ValueSource(ints = {128, 512, 2056})
  void getConfiguredSnapshotProfilerStackDepth(int depth) {
    var properties =
        DefaultConfigProperties.create(
            Map.of("splunk.snapshot.profiler.max.stack.depth", String.valueOf(depth)));
    assertEquals(depth, SnapshotProfilingConfiguration.getSnapshotProfilerStackDepth(properties));
  }

  @Test
  void getDefaultSnapshotProfilerStackDepthWhenNotSpecified() {
    var properties = DefaultConfigProperties.create(Collections.emptyMap());
    assertEquals(1024, SnapshotProfilingConfiguration.getSnapshotProfilerStackDepth(properties));
  }

  @ParameterizedTest
  @ValueSource(ints = {128, 512, 2056})
  void getConfiguredSnapshotProfilerSamplingInterval(int milliseconds) {
    var properties =
        DefaultConfigProperties.create(
            Map.of("splunk.snapshot.profiler.sampling.interval", String.valueOf(milliseconds)));
    assertEquals(
        Duration.ofMillis(milliseconds),
        SnapshotProfilingConfiguration.getSnapshotProfilerSamplingInterval(properties));
  }

  @Test
  void getDefaultSnapshotProfilerSamplingInterval() {
    var properties = DefaultConfigProperties.create(Collections.emptyMap());
    assertEquals(
        Duration.ofMillis(10),
        SnapshotProfilingConfiguration.getSnapshotProfilerSamplingInterval(properties));
  }

  @ParameterizedTest
  @ValueSource(ints = {128, 512, 2056})
  void getConfiguredSnapshotProfilerEmptyStagingInterval(int milliseconds) {
    var properties =
        DefaultConfigProperties.create(
            Map.of("splunk.snapshot.profiler.export.interval", String.valueOf(milliseconds)));
    assertEquals(
        Duration.ofMillis(milliseconds),
        SnapshotProfilingConfiguration.getSnapshotProfilerExportInterval(properties));
  }

  @Test
  void getDefaultSnapshotProfilerEmptyStagingInterval() {
    var properties = DefaultConfigProperties.create(Collections.emptyMap());
    assertEquals(
        Duration.ofSeconds(5),
        SnapshotProfilingConfiguration.getSnapshotProfilerExportInterval(properties));
  }

  @ParameterizedTest
  @ValueSource(ints = {100, 1000, 10_000})
  void getConfiguredSnapshotProfilerStagingCapacity(int value) {
    var properties =
        DefaultConfigProperties.create(
            Map.of("splunk.snapshot.profiler.staging.capacity", String.valueOf(value)));
    assertEquals(
        value, SnapshotProfilingConfiguration.getSnapshotProfilerStagingCapacity(properties));
  }

  @Test
  void getDefaultSnapshotProfilerStagingCapacity() {
    var properties = DefaultConfigProperties.create(Collections.emptyMap());
    assertEquals(
        2000, SnapshotProfilingConfiguration.getSnapshotProfilerStagingCapacity(properties));
  }

  @Nested
  class LoggingTest {
    @RegisterExtension
    private final LogCapturer log =
        LogCapturer.create().captureForType(SnapshotProfilingConfiguration.class, Level.DEBUG);

    @Test
    void includeSnapshotProfilingHeading() {
      var properties = DefaultConfigProperties.create(Collections.emptyMap());

      SnapshotProfilingConfiguration.log(properties);

      log.assertContains("-----------------------");
      log.assertContains("Snapshot Profiler Configuration:");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void includeSnapshotProfilingEnabled(boolean enabled) {
      var properties =
          DefaultConfigProperties.create(
              Map.of("splunk.snapshot.profiler.enabled", String.valueOf(enabled)));
      SnapshotProfilingConfiguration.log(properties);
      log.assertContains("splunk.snapshot.profiler.enabled" + " : " + enabled);
    }

    @ParameterizedTest
    @ValueSource(doubles = {.01, .05, .1})
    void includeSnapshotProfilingSelectionRate(double rate) {
      var properties =
          DefaultConfigProperties.create(
              Map.of("splunk.snapshot.selection.rate", String.valueOf(rate)));
      SnapshotProfilingConfiguration.log(properties);
      log.assertContains("splunk.snapshot.selection.rate" + " : " + rate);
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 1000, 10_1000})
    void includeSnapshotProfilingStackTraceDepth(int depth) {
      var properties =
          DefaultConfigProperties.create(
              Map.of("splunk.snapshot.profiler.max.stack.depth", String.valueOf(depth)));
      SnapshotProfilingConfiguration.log(properties);
      log.assertContains("splunk.snapshot.profiler.max.stack.depth" + " : " + depth);
    }

    @ParameterizedTest
    @ValueSource(strings = {"5ms", "10ms", "100ms"})
    void includeSnapshotProfilingSamplingInterval(String interval) {
      var properties =
          DefaultConfigProperties.create(
              Map.of("splunk.snapshot.profiler.sampling.interval", interval));

      SnapshotProfilingConfiguration.log(properties);

      var duration = properties.getDuration("splunk.snapshot.profiler.sampling.interval");
      log.assertContains("splunk.snapshot.profiler.sampling.interval" + " : " + duration);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10s", "30s", "10m"})
    void includeSnapshotProfilingExportInterval(String interval) {
      var properties =
          DefaultConfigProperties.create(
              Map.of("splunk.snapshot.profiler.export.interval", interval));

      SnapshotProfilingConfiguration.log(properties);

      var duration = properties.getDuration("splunk.snapshot.profiler.export.interval");
      log.assertContains("splunk.snapshot.profiler.export.interval" + " : " + duration);
    }

    @ParameterizedTest
    @ValueSource(ints = {1000, 2000, 10_1000})
    void includeSnapshotProfilingStagingCapacity(int capacity) {
      var properties =
          DefaultConfigProperties.create(
              Map.of("splunk.snapshot.profiler.staging.capacity", String.valueOf(capacity)));
      SnapshotProfilingConfiguration.log(properties);
      log.assertContains("splunk.snapshot.profiler.staging.capacity" + " : " + capacity);
    }
  }
}
