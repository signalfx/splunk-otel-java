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

package com.splunk.opentelemetry.sampler;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.Test;

class RuleBasedSamplerProviderConfigTest {

  @Test
  void shouldUseDefaultsOnEmptyConf() {
    RuleBasedSamplerProvider.Config config = new RuleBasedSamplerProvider.Config("");

    assertDefaultConf(config);
  }

  private void assertDefaultConf(RuleBasedSamplerProvider.Config config) {
    assertThat(config.drop).isEmpty();
    assertThat(config.fallback).isEqualTo(Sampler.parentBased(Sampler.alwaysOn()));
  }

  @Test
  void shouldMergeSeveralDrops() {
    RuleBasedSamplerProvider.Config config =
        new RuleBasedSamplerProvider.Config("drop=qwer;drop=/front");

    assertThat(config.drop).containsExactly("qwer", "/front");
    assertThat(config.fallback).isEqualTo(Sampler.parentBased(Sampler.alwaysOn()));
  }

  @Test
  void shouldConfigureFallback() {
    RuleBasedSamplerProvider.Config config =
        new RuleBasedSamplerProvider.Config("drop=qwer;fallback=always_off");

    assertThat(config.drop).containsExactly("qwer");
    assertThat(config.fallback).isEqualTo(Sampler.alwaysOff());
  }

  @Test
  void invalidRulesAreIgnored() {
    assertDefaultConf(new RuleBasedSamplerProvider.Config("drop;fallback=always_off"));
    assertDefaultConf(new RuleBasedSamplerProvider.Config("allow=we;fallbackToTricks"));
    assertDefaultConf(new RuleBasedSamplerProvider.Config("allow=fallback"));
  }
}
