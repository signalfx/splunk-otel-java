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

import static java.util.logging.Level.INFO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** This class is for file based configuration */
@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class RuleBasedSamplerComponentProvider implements ComponentProvider<Sampler> {
  private static final Logger logger =
      Logger.getLogger(RuleBasedSamplerComponentProvider.class.getName());

  @Override
  public Class<Sampler> getType() {
    return Sampler.class;
  }

  @Override
  public String getName() {
    return "rules";
  }

  @Override
  public Sampler create(DeclarativeConfigProperties samplerProperties) {
    DeclarativeConfigProperties samplerConfigProperties = samplerProperties.getStructured("config");
    Map<String, Object> configPropertiesMap =
        samplerConfigProperties != null
            ? DeclarativeConfigProperties.toMap(samplerConfigProperties)
            : new HashMap<>();
    ObjectMapper objectMapper = new ObjectMapper();
    RuleBasedSamplerConfig config =
        objectMapper.convertValue(configPropertiesMap, RuleBasedSamplerConfig.class);

    logger.log(INFO, "Received following rules: {0}", config);

    return RuleBasedSamplerFactory.create(config.fallback, config.drop);
  }

  private static class RuleBasedSamplerConfig {
    @JsonProperty("fallback")
    private String fallback = "parentbased_always_on";

    @JsonProperty("drop")
    private List<String> drop = new ArrayList<>();

    @Override
    public String toString() {
      return "RuleBasedSamplerConfig{" + "drop=" + drop + ", fallback=" + fallback + '}';
    }
  }
}
