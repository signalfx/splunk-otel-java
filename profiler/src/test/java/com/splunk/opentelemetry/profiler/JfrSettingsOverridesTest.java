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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JfrSettingsOverridesTest {

  @Test
  void shouldOverride_memoryEnabled_noAllocationSampling() {
    // given
    ProfilerConfiguration config = mock(ProfilerConfiguration.class);
    when(config.getCallStackInterval()).thenReturn(Duration.ofMillis(163));
    when(config.getMemoryEnabled()).thenReturn(true);
    when(config.getMemoryEventRateLimitEnabled()).thenReturn(false);
    when(config.getUseAllocationSampleEvent()).thenReturn(false);

    JfrSettingsOverrides overrides = new JfrSettingsOverrides(config);
    Map<String, String> jfrSettings =
        Map.of(
            "jdk.ThreadDump#period", "12",
            "jdk.ThreadDump#enabled", "true");

    // when
    Map<String, String> result = overrides.apply(jfrSettings);

    // then
    assertNotSame(result, jfrSettings);
    assertThat(result.get("jdk.ThreadDump#period")).isEqualTo("163 ms");
    assertThat(result.get("jdk.ThreadDump#enabled")).isEqualTo("true");
    assertThat(result.get("jdk.ObjectAllocationInNewTLAB#enabled")).isEqualTo("true");
    assertThat(result.get("jdk.ObjectAllocationOutsideTLAB#enabled")).isEqualTo("true");
    assertThat(result).hasSize(4);
  }

  @Test
  void shouldOverride_memoryEnabled_allocationSampling() {
    // given
    ProfilerConfiguration config = mock(ProfilerConfiguration.class);
    when(config.getCallStackInterval()).thenReturn(Duration.ofMillis(163));
    when(config.getMemoryEnabled()).thenReturn(true);
    when(config.getMemoryEventRateLimitEnabled()).thenReturn(true);
    when(config.getUseAllocationSampleEvent()).thenReturn(true);
    when(config.getMemoryEventRate()).thenReturn("200/s");

    JfrSettingsOverrides overrides = new JfrSettingsOverrides(config);
    Map<String, String> jfrSettings =
        Map.of(
            "jdk.ThreadDump#period", "12",
            "jdk.ThreadDump#enabled", "true",
            "jdk.ObjectAllocationSample#enabled", "false",
            "jdk.ObjectAllocationSample#throttle", "123/s");

    // when
    Map<String, String> result = overrides.apply(jfrSettings);

    // then
    assertNotSame(result, jfrSettings);
    assertThat(result.get("jdk.ThreadDump#period")).isEqualTo("163 ms");
    assertThat(result.get("jdk.ThreadDump#enabled")).isEqualTo("true");
    assertThat(result.get("jdk.ObjectAllocationSample#enabled")).isEqualTo("true");
    assertThat(result.get("jdk.ObjectAllocationSample#throttle")).isEqualTo("200/s");
    assertThat(result).hasSize(4);
  }

  @Test
  void shouldNotOverrideWhenMemoryDisabledAndIntervalIsZero() {
    // given
    ProfilerConfiguration config = mock(ProfilerConfiguration.class);
    when(config.getCallStackInterval()).thenReturn(Duration.ofMillis(0));
    when(config.getMemoryEnabled()).thenReturn(false);

    JfrSettingsOverrides overrides = new JfrSettingsOverrides(config);
    Map<String, String> jfrSettings =
        Map.of(
            "jdk.ThreadDump#period", "12",
            "jdk.ThreadDump#enabled", "false");

    // when
    Map<String, String> result = overrides.apply(jfrSettings);

    // then
    assertNotSame(result, jfrSettings);
    assertThat(result.get("jdk.ThreadDump#period")).isEqualTo("12");
    assertThat(result.get("jdk.ThreadDump#enabled")).isEqualTo("false");
    assertThat(result).hasSize(2);
  }
}
