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
import static org.assertj.core.api.Assertions.assertThat;

import com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.extension.incubator.fileconfig.YamlDeclarativeConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalLanguageSpecificInstrumentationPropertyModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SnapshotProfilingDeclarativeConfigurationTest {
  @Test
  void shouldMapYamlToConfiguration() {
    // given
    OpenTelemetryConfigurationModel model =
        DeclarativeConfigTestUtil.parse(
            """
            file_format: "1.0-rc.2"
            instrumentation/development:
              java:
                distribution:
                  splunk:
                    profiling:
                      callgraphs:                     # SPLUNK_SNAPSHOT_PROFILER_ENABLED
                        sampling_interval: 10         # SPLUNK_SNAPSHOT_SAMPLING_INTERVAL
                        export_interval: 20           # SPLUNK_SNAPSHOT_EXPORT_INTERVAL
                        selection_probability: 0.0123 # SPLUNK_SNAPSHOT_SELECTION_PROBABILITY
                        stack_depth: 200              # SPLUNK_SNAPSHOT_STACK_DEPTH
                        staging_capacity: 7           # SPLUNK_SNAPSHOT_STAGING_CAPACITY
            """);

    DeclarativeConfigProperties profilingConfig = getProfilingConfig(model);

    // when
    SnapshotProfilingDeclarativeConfiguration config =
        new SnapshotProfilingDeclarativeConfiguration(profilingConfig);

    // then
    assertThat(config.isEnabled()).isTrue();
    assertThat(config.getSnapshotSelectionProbability()).isEqualTo(0.0123);
    assertThat(config.getStackDepth()).isEqualTo(200);
    assertThat(config.getSamplingInterval()).isEqualTo(Duration.ofMillis(10));
    assertThat(config.getExportInterval()).isEqualTo(Duration.ofMillis(20));
    assertThat(config.getStagingCapacity()).isEqualTo(7);
  }

  @Test
  void shouldEnableSnapshotProfilingWithDefaults() {
    // given
    OpenTelemetryConfigurationModel model =
        DeclarativeConfigTestUtil.parse(
            """
            file_format: "1.0-rc.2"
            instrumentation/development:
              java:
                distribution:
                  splunk:
                    profiling:
                      callgraphs:                     # SPLUNK_SNAPSHOT_PROFILER_ENABLED
            """);

    DeclarativeConfigProperties snapshotProfilingConfig = getProfilingConfig(model);

    // when
    SnapshotProfilingDeclarativeConfiguration config =
        new SnapshotProfilingDeclarativeConfiguration(snapshotProfilingConfig);

    // then
    assertThat(config.isEnabled()).isTrue();
    assertThat(config.getSnapshotSelectionProbability()).isEqualTo(0.01);
    assertThat(config.getStackDepth()).isEqualTo(1024);
    assertThat(config.getSamplingInterval()).isEqualTo(Duration.ofMillis(10));
    assertThat(config.getExportInterval()).isEqualTo(Duration.ofSeconds(5));
    assertThat(config.getStagingCapacity()).isEqualTo(2000);
  }

  @Test
  void shouldDisableSnapshotProfiling() {
    // given
    OpenTelemetryConfigurationModel model =
        DeclarativeConfigTestUtil.parse(
            """
            file_format: "1.0-rc.2"
            instrumentation/development:
              java:
                distribution:
                  splunk:
                    profiling:
            """);

    DeclarativeConfigProperties profilingConfig = getProfilingConfig(model);

    // when
    SnapshotProfilingDeclarativeConfiguration config =
        new SnapshotProfilingDeclarativeConfiguration(profilingConfig);

    // then
    assertThat(config.isEnabled()).isFalse();
  }

  @Test
  void shouldReturnValidSelectionInterval_aboveMax() {
    // given
    OpenTelemetryConfigurationModel model =
        DeclarativeConfigTestUtil.parse(
            """
            file_format: "1.0-rc.2"
            instrumentation/development:
              java:
                distribution:
                  splunk:
                    profiling:
                      callgraphs:
                        selection_probability: 1.1
            """);

    DeclarativeConfigProperties profilingConfig = getProfilingConfig(model);

    // when
    SnapshotProfilingDeclarativeConfiguration config =
        new SnapshotProfilingDeclarativeConfiguration(profilingConfig);

    // then
    assertThat(config.getSnapshotSelectionProbability())
        .isEqualTo(SnapshotProfilingConfiguration.MAX_SELECTION_PROBABILITY);
  }

  @Test
  void shouldReturnValidSelectionInterval_tooLow() {
    // given
    OpenTelemetryConfigurationModel model =
        DeclarativeConfigTestUtil.parse(
            """
            file_format: "1.0-rc.2"
            instrumentation/development:
              java:
                distribution:
                  splunk:
                    profiling:
                      callgraphs:
                        selection_probability: 0
            """);

    DeclarativeConfigProperties profilingConfig = getProfilingConfig(model);

    // when
    SnapshotProfilingDeclarativeConfiguration config =
        new SnapshotProfilingDeclarativeConfiguration(profilingConfig);

    // then
    assertThat(config.getSnapshotSelectionProbability())
        .isEqualTo(SnapshotProfilingConfiguration.DEFAULT_SELECTION_PROBABILITY);
  }

  private static DeclarativeConfigProperties getProfilingConfig(
      OpenTelemetryConfigurationModel model) {
    Map<String, ExperimentalLanguageSpecificInstrumentationPropertyModel> original =
        model.getInstrumentationDevelopment().getJava().getAdditionalProperties();
    Map<String, Object> properties =
        Map.of("distribution", original.get("distribution").getAdditionalProperties());
    ComponentLoader componentLoader =
        ComponentLoader.forClassLoader(DeclarativeConfigProperties.class.getClassLoader());
    DeclarativeConfigProperties declarativeConfigProperties =
        YamlDeclarativeConfigProperties.create(properties, componentLoader);
    return declarativeConfigProperties
        .getStructured("distribution", empty())
        .getStructured("splunk", empty())
        .getStructured("profiling", empty());
  }
}
