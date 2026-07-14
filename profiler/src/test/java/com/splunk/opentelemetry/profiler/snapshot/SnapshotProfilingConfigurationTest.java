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

import java.time.Duration;
import org.junit.jupiter.api.Test;

class SnapshotProfilingConfigurationTest {

  @Test
  void toBuilder_shouldCopyExistingConfigurationWithoutMutation() {
    Object configProperties = new Object();
    SnapshotProfilingConfiguration original =
        SnapshotProfilingConfiguration.builder()
            .setEnabled(true)
            .setSnapshotSelectionProbability(0.25)
            .setStackDepth(73)
            .setSamplingInterval(Duration.ofMillis(1410))
            .setExportInterval(Duration.ofSeconds(30))
            .setStagingCapacity(321)
            .setConfigProperties(configProperties)
            .build();

    SnapshotProfilingConfiguration copy = original.toBuilder().build();

    assertThat(copy).isNotSameAs(original);
    assertThat(copy).isEqualTo(original);
    assertThat(copy.hashCode()).isEqualTo(original.hashCode());
    assertThat(copy.getConfigProperties()).isSameAs(configProperties);
  }

  @Test
  void toBuilder_shouldCopyExistingConfigurationAndAllowMutation() {
    Object configProperties = new Object();
    Object mutatedConfigProperties = new Object();
    SnapshotProfilingConfiguration original =
        SnapshotProfilingConfiguration.builder()
            .setEnabled(false)
            .setSnapshotSelectionProbability(0.25)
            .setStackDepth(73)
            .setSamplingInterval(Duration.ofMillis(1410))
            .setExportInterval(Duration.ofSeconds(30))
            .setStagingCapacity(321)
            .setConfigProperties(configProperties)
            .build();

    SnapshotProfilingConfiguration copy =
        original.toBuilder()
            .setEnabled(true)
            .setSnapshotSelectionProbability(0.5)
            .setStackDepth(142)
            .setSamplingInterval(Duration.ofMillis(2500))
            .setExportInterval(Duration.ofSeconds(60))
            .setStagingCapacity(654)
            .setConfigProperties(mutatedConfigProperties)
            .build();

    assertThat(copy).isNotEqualTo(original);
    assertThat(copy.isEnabled()).isTrue();
    assertThat(copy.getSnapshotSelectionProbability()).isEqualTo(0.5);
    assertThat(copy.getStackDepth()).isEqualTo(142);
    assertThat(copy.getSamplingInterval()).isEqualTo(Duration.ofMillis(2500));
    assertThat(copy.getExportInterval()).isEqualTo(Duration.ofSeconds(60));
    assertThat(copy.getStagingCapacity()).isEqualTo(654);
    assertThat(copy.getConfigProperties()).isSameAs(mutatedConfigProperties);
  }
}
