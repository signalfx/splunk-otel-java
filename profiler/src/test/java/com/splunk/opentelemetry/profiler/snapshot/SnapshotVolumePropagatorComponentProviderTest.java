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

import static com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil.getProfilingConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.splunk.opentelemetry.testing.declarativeconfig.DeclarativeConfigTestUtil;
import io.opentelemetry.api.incubator.config.DeclarativeConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SnapshotVolumePropagatorComponentProviderTest {
  @AfterEach
  void tearDown() {
    SnapshotProfilingDeclarativeConfiguration.SUPPLIER.reset();
  }

  @Test
  void shouldThrowExceptionWhenProfilingNotConfigured() {
    // given
    SnapshotVolumePropagatorComponentProvider propagatorProvider =
        new SnapshotVolumePropagatorComponentProvider();

    // when
    assertThatThrownBy(() -> propagatorProvider.create(null))
        .isInstanceOf(DeclarativeConfigException.class);
  }

  @Test
  void shouldCreatePropagatorWithDefaultSelectionProbabilityWhenNotProvided() {
    // given
    var yaml =
        """
            file_format: "1.0-rc.3"
            distribution:
              splunk:
                profiling:
                  callgraphs:
            """;
    var configurationModel = DeclarativeConfigTestUtil.parse(yaml);
    DeclarativeConfigProperties profilingConfig = getProfilingConfig(configurationModel);
    SnapshotProfilingDeclarativeConfiguration.SUPPLIER.configure(
        new SnapshotProfilingDeclarativeConfiguration(profilingConfig));

    SnapshotVolumePropagatorComponentProvider propagatorProvider =
        new SnapshotVolumePropagatorComponentProvider();
    propagatorProvider = spy(propagatorProvider);

    // when
    TextMapPropagator propagator = propagatorProvider.create(null);

    // then
    assertThat(propagator).isNotNull();
    assertThat(propagator).isInstanceOf(SnapshotVolumePropagator.class);
    verify(propagatorProvider)
        .selector(SnapshotProfilingConfiguration.DEFAULT_SELECTION_PROBABILITY);
  }

  @Test
  void shouldCreatePropagatorWithProvidedValidSelectionProbability() {
    // given
    var yaml =
        """
            file_format: "1.0-rc.3"
            distribution:
              splunk:
                profiling:
                  callgraphs:
                    selection_probability: 0.123
            """;
    var configurationModel = DeclarativeConfigTestUtil.parse(yaml);
    DeclarativeConfigProperties profilingConfig = getProfilingConfig(configurationModel);
    SnapshotProfilingDeclarativeConfiguration.SUPPLIER.configure(
        new SnapshotProfilingDeclarativeConfiguration(profilingConfig));

    SnapshotVolumePropagatorComponentProvider propagatorProvider =
        new SnapshotVolumePropagatorComponentProvider();
    propagatorProvider = spy(propagatorProvider);

    // when
    TextMapPropagator propagator = propagatorProvider.create(null);

    // then
    assertThat(propagator).isNotNull();
    assertThat(propagator).isInstanceOf(SnapshotVolumePropagator.class);
    verify(propagatorProvider).selector(0.123);
  }
}
