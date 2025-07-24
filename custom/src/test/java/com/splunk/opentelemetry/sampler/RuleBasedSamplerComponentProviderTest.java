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

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;

class RuleBasedSamplerComponentProviderTest {
  @Test
  void shouldCreateSamplerFromFullConfig() {
    // given
    RuleBasedSamplerComponentProvider ruleBasedSamplerComponentProvider =
        new RuleBasedSamplerComponentProvider();
    String yaml =
        """
        config:
          fallback: always_on
          drop:
            - test1
            - test2
        """;
    DeclarativeConfigProperties samplerConfigProperties =
        DeclarativeConfiguration.toConfigProperties(new ByteArrayInputStream(yaml.getBytes()));

    // when
    Sampler sampler = ruleBasedSamplerComponentProvider.create(samplerConfigProperties);

    // then
    assertThat(sampler).isNotNull();
    String description = sampler.getDescription();
    assertThat(description).isNotNull();
    assertThat(description).contains("pattern=test1");
    assertThat(description).contains("pattern=test2");
    assertThat(description).contains("fallback=AlwaysOnSampler");
  }

  @Test
  void shouldCreateSamplerFromMinimalConfig() {
    // given
    RuleBasedSamplerComponentProvider ruleBasedSamplerComponentProvider =
        new RuleBasedSamplerComponentProvider();
    String yaml =
        """
        config:
        """;
    DeclarativeConfigProperties samplerConfigProperties =
        DeclarativeConfiguration.toConfigProperties(new ByteArrayInputStream(yaml.getBytes()));

    // when
    Sampler sampler = ruleBasedSamplerComponentProvider.create(samplerConfigProperties);

    // then
    assertThat(sampler).isNotNull();
    String description = sampler.getDescription();
    assertThat(description).isNotNull();
    assertThat(description).contains("rules=[]");
    assertThat(description).contains("fallback=ParentBased{root:AlwaysOnSampler");
  }
}
