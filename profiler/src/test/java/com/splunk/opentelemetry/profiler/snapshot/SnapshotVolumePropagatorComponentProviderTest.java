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

import com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.extension.incubator.fileconfig.YamlDeclarativeConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.TextMapPropagatorPropertyModel;
import org.junit.jupiter.api.Test;

class SnapshotVolumePropagatorComponentProviderTest {

  @Test
  void shouldCreatePropagatorWithDefaultSelectionProbabilityWhenNotProvided() {
    // given
    var yaml =
        """
            file_format: "1.0-rc.2"
            propagator:
              composite:
                - splunk_snapshot_volume:
            """;
    var propagatorProperties = getPropagatorProperties(DeclarativeConfigTestUtil.parse(yaml));
    SnapshotVolumePropagatorComponentProvider propagatorProvider =
        new SnapshotVolumePropagatorComponentProvider();

    // when
    TextMapPropagator propagator = propagatorProvider.create(propagatorProperties);

    // then
    assertThat(propagator).isNotNull();
    assertThat(propagator).isInstanceOf(SnapshotVolumePropagator.class);
  }

  @Test
  void shouldCreatePropagatorWithProvidedValidSelectionProbability() {
    // given
    var yaml =
        """
            file_format: "1.0-rc.2"
            propagator:
              composite:
                - splunk_snapshot_volume:
                    snapshot_selection_probability: 0.1
            """;
    var propagatorProperties = getPropagatorProperties(DeclarativeConfigTestUtil.parse(yaml));
    SnapshotVolumePropagatorComponentProvider propagatorProvider =
        new SnapshotVolumePropagatorComponentProvider();

    // when
    TextMapPropagator propagator = propagatorProvider.create(propagatorProperties);

    // then
    assertThat(propagator).isNotNull();
    assertThat(propagator).isInstanceOf(SnapshotVolumePropagator.class);
  }

  static DeclarativeConfigProperties getPropagatorProperties(
      OpenTelemetryConfigurationModel model) {
    TextMapPropagatorPropertyModel config =
        model
            .getPropagator()
            .getComposite()
            .get(0)
            .getAdditionalProperties()
            .get("splunk_snapshot_volume");
    if (config == null) {
      return DeclarativeConfigProperties.empty();
    }
    return YamlDeclarativeConfigProperties.create(config.getAdditionalProperties(), null);
  }
}
