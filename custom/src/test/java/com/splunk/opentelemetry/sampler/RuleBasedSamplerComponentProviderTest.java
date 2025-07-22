package com.splunk.opentelemetry.sampler;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedSamplerComponentProviderTest {
  @Test
  void shouldCreateSamplerFromFullConfig() {
    // given
    RuleBasedSamplerComponentProvider ruleBasedSamplerComponentProvider = new RuleBasedSamplerComponentProvider();
    String yaml = """
        config:
          fallback: always_on
          drop:
            - test1
            - test2
        """;
    DeclarativeConfigProperties samplerConfigProperties = DeclarativeConfiguration.toConfigProperties(new ByteArrayInputStream(yaml.getBytes()));

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
    RuleBasedSamplerComponentProvider ruleBasedSamplerComponentProvider = new RuleBasedSamplerComponentProvider();
    String yaml = """
        config:
        """;
    DeclarativeConfigProperties samplerConfigProperties = DeclarativeConfiguration.toConfigProperties(new ByteArrayInputStream(yaml.getBytes()));

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
