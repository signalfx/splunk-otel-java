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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StackTraceSamplerInitializerTest {
  @AfterEach
  void cleanupSuppliers() {
    StackTraceSampler.SUPPLIER.get().close();
    StagingArea.SUPPLIER.get().close();
    StackTraceSampler.SUPPLIER.reset();
    StagingArea.SUPPLIER.reset();
  }

  @Test
  void setupStackTraceSamplerConfiguresDefaults() {
    // given
    SnapshotProfilingConfiguration configuration = mock(SnapshotProfilingConfiguration.class);
    when(configuration.getStagingCapacity()).thenReturn(15);

    // when
    StackTraceSamplerInitializer.setupStackTraceSampler(configuration);

    // then
    StackTraceSampler configuredSampler = StackTraceSampler.SUPPLIER.get();
    StagingArea configuredStagingArea = StagingArea.SUPPLIER.get();
    assertThat(configuredSampler).isInstanceOf(PeriodicStackTraceSampler.class);
    assertThat(configuredStagingArea).isInstanceOf(PeriodicallyExportingStagingArea.class);
  }
}
