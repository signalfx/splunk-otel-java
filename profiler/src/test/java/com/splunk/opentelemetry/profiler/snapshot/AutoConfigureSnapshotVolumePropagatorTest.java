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

import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkExtension;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkExtension.Builder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AutoConfigureSnapshotVolumePropagatorTest {
  private static final String OTEL_PROPAGATORS = "otel-propagators";

  @Test
  void autoConfigureSnapshotVolumePropagator() {
    try (var sdk = newSdk().build()) {
      var properties = sdk.getConfig();
      assertThat(properties.getList(OTEL_PROPAGATORS))
          .contains(SnapshotVolumePropagatorProvider.NAME);
    }
  }

  @Test
  void snapshotVolumePropagatorMustBeAfterTraceContextAndBaggage() {
    try (var sdk = newSdk().build()) {
      var properties = sdk.getConfig();
      assertThat(properties.getList(OTEL_PROPAGATORS))
          .containsExactly("tracecontext", "baggage", SnapshotVolumePropagatorProvider.NAME);
    }
  }

  @Test
  void appendSnapshotPropagatorToEndOfAlreadyConfiguredPropagators() {
    try (var sdk =
        newSdk()
            .withProperty(OTEL_PROPAGATORS, "tracecontext,baggage,some-other-propagator")
            .build()) {
      var properties = sdk.getConfig();
      assertThat(properties.getList(OTEL_PROPAGATORS))
          .containsExactly(
              "tracecontext",
              "baggage",
              "some-other-propagator",
              SnapshotVolumePropagatorProvider.NAME);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"baggage", "tracecontext"})
  void doNotDoubleCountDefaultOpenTelemetryPropagators(String propagatorName) {
    try (var sdk = newSdk().withProperty(OTEL_PROPAGATORS, propagatorName).build()) {
      var properties = sdk.getConfig();
      assertThat(properties.getList(OTEL_PROPAGATORS)).containsOnlyOnce(propagatorName);
    }
  }

  @Test
  void doNotDoubleCountSnapshotVolumePropagator() {
    try (var sdk =
        newSdk().withProperty(OTEL_PROPAGATORS, SnapshotVolumePropagatorProvider.NAME).build()) {
      var properties = sdk.getConfig();
      assertThat(properties.getList(OTEL_PROPAGATORS))
          .containsOnlyOnce(SnapshotVolumePropagatorProvider.NAME);
    }
  }

  @Test
  void doNotAddSnapshotVolumePropagatorWhenTraceSnapshottingIsDisabled() {
    try (var sdk =
        newSdk()
            .withProperty(
                SnapshotProfilingEnvVarsConfiguration.CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER, "false")
            .build()) {
      var properties = sdk.getConfig();
      assertThat(properties.getList(OTEL_PROPAGATORS))
          .doesNotContain(SnapshotVolumePropagatorProvider.NAME);
    }
  }

  @Test
  void doNotAddSnapshotVolumePropagatorsConfiguredAsNone() {
    try (var sdk = newSdk().withProperty(OTEL_PROPAGATORS, "none").build()) {
      var properties = sdk.getConfig();
      assertThat(properties.getList(OTEL_PROPAGATORS))
          .doesNotContain(SnapshotVolumePropagatorProvider.NAME);
    }
  }

  @Test
  void doNotAddTraceContextPropagatorWhenOtherPropagatorsAreExplicitlyConfigured() {
    try (var sdk =
        newSdk().withProperty(OTEL_PROPAGATORS, "some-other-propagator,baggage").build()) {
      var properties = sdk.getConfig();
      assertThat(properties.getList(OTEL_PROPAGATORS))
          .containsExactly(
              "some-other-propagator", "baggage", SnapshotVolumePropagatorProvider.NAME);
    }
  }

  @Test
  void addBaggagePropagatorWhenOtherPropagatorsAreExplicitlyConfiguredButBaggageIsMissing() {
    try (var sdk = newSdk().withProperty(OTEL_PROPAGATORS, "some-other-propagator").build()) {
      var properties = sdk.getConfig();
      assertThat(properties.getList(OTEL_PROPAGATORS))
          .containsExactly(
              "some-other-propagator", "baggage", SnapshotVolumePropagatorProvider.NAME);
    }
  }

  private Builder newSdk() {
    return OpenTelemetrySdkExtension.configure()
        .withProperty(
            SnapshotProfilingEnvVarsConfiguration.CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER, "true")
        .with(new SnapshotProfilingSdkCustomizer());
  }
}
