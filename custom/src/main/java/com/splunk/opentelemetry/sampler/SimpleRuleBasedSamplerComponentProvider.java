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

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

// TODO: Is this class needed? see:
//  https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/samplers
//  https://youtu.be/u6svjtGpXO4?t=1262,
//  The same effect can be achieved with YAML not using our :
//    rule_based_routing:
//      fallback_sampler:
//        always_off:
//      span_kind: SERVER
//      rules:
//        - action: DROP
//          attribute: url.path
//          pattern: /url1.*
//        - action: DROP
//          attribute: url.path
//          pattern: /url2

@SuppressWarnings("rawtypes")
@AutoService(ComponentProvider.class)
public class SimpleRuleBasedSamplerComponentProvider implements ComponentProvider<Sampler> {
  private static final Logger logger =
      Logger.getLogger(SimpleRuleBasedSamplerComponentProvider.class.getName());

  @Override
  public Class<Sampler> getType() {
    return Sampler.class;
  }

  @Override
  public String getName() {
    return "simple_rule_based_routing";
  }

  @Override
  public Sampler create(DeclarativeConfigProperties samplerProperties) {
    DeclarativeConfigProperties samplerConfigProperties = samplerProperties.getStructured("config");
    Map<String, Object> configPropertiesMap =
        samplerConfigProperties != null
            ? DeclarativeConfigProperties.toMap(samplerConfigProperties)
            : new HashMap<>();

    logger.log(INFO, "Received following rules: {0}", configPropertiesMap);

    Object fallbackProperty = configPropertiesMap.get("fallback");
    String fallback =
        fallbackProperty != null ? fallbackProperty.toString() : "parentbased_always_on";

    List<String> drop = (List<String>) configPropertiesMap.get("drop");
    drop = drop != null ? drop : Collections.emptyList();

    return RuleBasedSamplerFactory.create(fallback, drop);
  }
}
