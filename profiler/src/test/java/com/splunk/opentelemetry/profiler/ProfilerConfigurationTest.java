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

package com.splunk.opentelemetry.profiler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ProfilerConfigurationTest {

  @Test
  void newBuilderCopiesExistingConfigurationAndAllowsMutation() {
    Object configProperties = new Object();
    ProfilerConfiguration original =
        ProfilerConfiguration.builder()
            .setEnabled(false)
            .setIngestUrl("https://logs.example.com")
            .setOtlpProtocol("grpc")
            .setMemoryEnabled(true)
            .setMemoryEventRateLimitEnabled(false)
            .setMemoryEventRate("250/s")
            .setUseAllocationSampleEvent(true)
            .setCallStackInterval(Duration.ofMillis(1410))
            .setIncludeAgentInternalStacks(true)
            .setIncludeJvmInternalStacks(true)
            .setTracingStacksOnly(true)
            .setStackDepth(73)
            .setKeepFiles(true)
            .setProfilerDirectory("/tmp/profiler")
            .setRecordingDuration(Duration.ofSeconds(30))
            .setConfigProperties(configProperties)
            .build();

    ProfilerConfiguration copy = original.newBuilder().setEnabled(true).build();

    assertThat(original.isEnabled()).isFalse();
    assertThat(copy.isEnabled()).isTrue();
    assertThat(copy.getIngestUrl()).isEqualTo(original.getIngestUrl());
    assertThat(copy.getOtlpProtocol()).isEqualTo(original.getOtlpProtocol());
    assertThat(copy.getMemoryEnabled()).isEqualTo(original.getMemoryEnabled());
    assertThat(copy.getMemoryEventRateLimitEnabled())
        .isEqualTo(original.getMemoryEventRateLimitEnabled());
    assertThat(copy.getMemoryEventRate()).isEqualTo(original.getMemoryEventRate());
    assertThat(copy.getUseAllocationSampleEvent())
        .isEqualTo(original.getUseAllocationSampleEvent());
    assertThat(copy.getCallStackInterval()).isEqualTo(original.getCallStackInterval());
    assertThat(copy.getIncludeAgentInternalStacks())
        .isEqualTo(original.getIncludeAgentInternalStacks());
    assertThat(copy.getIncludeJvmInternalStacks())
        .isEqualTo(original.getIncludeJvmInternalStacks());
    assertThat(copy.getTracingStacksOnly()).isEqualTo(original.getTracingStacksOnly());
    assertThat(copy.getStackDepth()).isEqualTo(original.getStackDepth());
    assertThat(copy.getKeepFiles()).isEqualTo(original.getKeepFiles());
    assertThat(copy.getProfilerDirectory()).isEqualTo(original.getProfilerDirectory());
    assertThat(copy.getRecordingDuration()).isEqualTo(original.getRecordingDuration());
    assertThat(copy.getConfigProperties()).isSameAs(configProperties);
  }
}
