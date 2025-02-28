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

import com.splunk.opentelemetry.profiler.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AutoConfigureSnapshotVolumePropagatorTest {
  @Test
  void autoConfigureSnapshotVolumePropagator() {
    try (var sdk = newSdk().build()) {
      var properties = sdk.getProperties();
      assertThat(properties.getList("otel.propagators")).contains("splunk-snapshot");
    }
  }

  @Test
  void snapshotVolumePropagatorMustBeAfterBaggageAndTraceContext() {
    try (var sdk = newSdk().build()) {
      var properties = sdk.getProperties();
      assertThat(properties.getList("otel.propagators"))
          .containsExactly("baggage", "tracecontext", "splunk-snapshot");
    }
  }

  @Test
  void appendSnapshotPropagatorToEndOfAlreadyConfiguredPropagators() {
    try (var sdk =
        newSdk()
            .withProperty("otel.propagators", "baggage,tracecontext,some-other-propagator")
            .build()) {
      var properties = sdk.getProperties();
      assertThat(properties.getList("otel.propagators"))
          .containsExactly("baggage", "tracecontext", "some-other-propagator", "splunk-snapshot");
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"baggage", "tracecontext"})
  void doNotDoubleCountDefaultOpenTelemetryPropagators(String propagatorName) {
    try (var sdk = newSdk().withProperty("otel.propagators", propagatorName).build()) {
      var properties = sdk.getProperties();
      assertThat(properties.getList("otel.propagators")).containsOnlyOnce(propagatorName);
    }
  }

  @Test
  void doNotDoubleCountSnapshotVolumePropagator() {
    try (var sdk = newSdk().withProperty("otel.propagators", "splunk-snapshot").build()) {
      var properties = sdk.getProperties();
      assertThat(properties.getList("otel.propagators")).containsOnlyOnce("splunk-snapshot");
    }
  }

  @Test
  void doNotAddSnapshotVolumePropagatorWhenTraceSnapshottingIsDisabled() {
    try (var sdk =
        newSdk().withProperty(Configuration.CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER, "false").build()) {
      var properties = sdk.getProperties();
      assertThat(properties.getList("otel.propagators")).doesNotContain("splunk-snapshot");
    }
  }

  private OpenTelemetrySdkExtension.Builder newSdk() {
    return OpenTelemetrySdkExtension.builder()
        .withProperty(Configuration.CONFIG_KEY_ENABLE_SNAPSHOT_PROFILER, "true")
        .with(new SnapshotProfilingSdkCustomizer());
  }
}
