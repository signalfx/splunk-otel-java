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
  void toBuilder_shouldCopyExistingConfigurationWithoutMutation() {
    Object configProperties = new Object();
    ProfilerConfiguration original =
        ProfilerConfiguration.builder()
            .setEnabled(true)
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

    ProfilerConfiguration copy = original.toBuilder().build();

    assertThat(copy).isNotSameAs(original);
    assertThat(copy).usingRecursiveComparison().isEqualTo(original);
    assertThat(copy.getConfigProperties()).isSameAs(configProperties);
  }

  @Test
  void toBuilder_shouldCopyExistingConfigurationAndAllowMutation() {
    Object configProperties = new Object();
    Object mutatedConfigProperties = new Object();
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

    ProfilerConfiguration copy =
        original.toBuilder()
            .setEnabled(true)
            .setIngestUrl("https://mutated-logs.example.com")
            .setOtlpProtocol("http/protobuf")
            .setMemoryEnabled(false)
            .setMemoryEventRateLimitEnabled(true)
            .setMemoryEventRate("333/s")
            .setUseAllocationSampleEvent(false)
            .setCallStackInterval(Duration.ofMillis(2500))
            .setIncludeAgentInternalStacks(false)
            .setIncludeJvmInternalStacks(false)
            .setTracingStacksOnly(false)
            .setStackDepth(142)
            .setKeepFiles(false)
            .setProfilerDirectory("/tmp/mutated-profiler")
            .setRecordingDuration(Duration.ofSeconds(60))
            .setConfigProperties(mutatedConfigProperties)
            .build();

    assertThat(copy.isEnabled()).isTrue();
    assertThat(copy.getIngestUrl()).isEqualTo("https://mutated-logs.example.com");
    assertThat(copy.getOtlpProtocol()).isEqualTo("http/protobuf");
    assertThat(copy.getMemoryEnabled()).isFalse();
    assertThat(copy.getMemoryEventRateLimitEnabled()).isTrue();
    assertThat(copy.getMemoryEventRate()).isEqualTo("333/s");
    assertThat(copy.getUseAllocationSampleEvent()).isFalse();
    assertThat(copy.getCallStackInterval()).isEqualTo(Duration.ofMillis(2500));
    assertThat(copy.getIncludeAgentInternalStacks()).isFalse();
    assertThat(copy.getIncludeJvmInternalStacks()).isFalse();
    assertThat(copy.getTracingStacksOnly()).isFalse();
    assertThat(copy.getStackDepth()).isEqualTo(142);
    assertThat(copy.getKeepFiles()).isFalse();
    assertThat(copy.getProfilerDirectory()).isEqualTo("/tmp/mutated-profiler");
    assertThat(copy.getRecordingDuration()).isEqualTo(Duration.ofSeconds(60));
    assertThat(copy.getConfigProperties()).isSameAs(mutatedConfigProperties);
  }
}
