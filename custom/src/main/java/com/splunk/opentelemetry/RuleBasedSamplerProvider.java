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

package com.splunk.opentelemetry;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.contrib.samplers.RuleBasedRoutingSampler;
import io.opentelemetry.contrib.samplers.RuleBasedRoutingSamplerBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(ConfigurableSamplerProvider.class)
public class RuleBasedSamplerProvider implements ConfigurableSamplerProvider {
  private static final Logger log = LoggerFactory.getLogger(RuleBasedSamplerProvider.class);
  public static final String SAMPLER_ARG = "otel.traces.sampler.arg";

  @Override
  public Sampler createSampler(ConfigProperties config) {
    String configString = config.getString(SAMPLER_ARG);
    Config samplerConfiguration = new Config(configString);

    log.info("Received following rules: {}", samplerConfiguration);

    RuleBasedRoutingSamplerBuilder builder =
        RuleBasedRoutingSampler.builder(SpanKind.SERVER, samplerConfiguration.fallback);
    samplerConfiguration.drop.forEach(d -> builder.drop(SemanticAttributes.HTTP_TARGET, d));

    return builder.build();
  }

  @Override
  public String getName() {
    return "rules";
  }

  // Visible for tests
  static class Config {
    final List<String> drop = new ArrayList<>();
    Sampler fallback = Sampler.parentBased(Sampler.alwaysOn());

    public Config(String config) {
      if (config == null) {
        return;
      }

      try {
        String[] parts = config.split(";");
        for (String part : parts) {
          if (part.startsWith("fallback")) {
            fallback = selectSampler(part.split("=")[1]);
          } else if (part.startsWith("drop")) {
            drop.add(part.split("=")[1]);
          }
        }
      } catch (RuntimeException ex) {
        log.warn("Failed to parse {} configuration option. Using default conf", SAMPLER_ARG, ex);
      }
    }

    @Override
    public String toString() {
      return "Config{" + "drop=" + drop + ", fallback=" + fallback + '}';
    }

    private Sampler selectSampler(String samplerName) {
      switch (samplerName) {
        case "always_on":
          return Sampler.alwaysOn();
        case "always_off":
          return Sampler.alwaysOff();
        case "parentbased_always_on":
          return Sampler.parentBased(Sampler.alwaysOn());
        case "parentbased_always_off":
          return Sampler.parentBased(Sampler.alwaysOff());
        default:
          throw new IllegalArgumentException("Unsupported fallback sampler " + samplerName);
      }
    }
  }
}
