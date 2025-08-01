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
import static java.util.logging.Level.WARNING;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.contrib.sampler.RuleBasedRoutingSampler;
import io.opentelemetry.contrib.sampler.RuleBasedRoutingSamplerBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.UrlAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@AutoService(ConfigurableSamplerProvider.class)
public class RuleBasedSamplerProvider implements ConfigurableSamplerProvider {
  private static final Logger logger = Logger.getLogger(RuleBasedSamplerProvider.class.getName());
  public static final String SAMPLER_ARG = "otel.traces.sampler.arg";

  @Override
  public Sampler createSampler(ConfigProperties config) {
    String configString = config.getString(SAMPLER_ARG);
    Config samplerConfiguration = new Config(configString);

    logger.log(INFO, "Received following rules: {0}", samplerConfiguration);

    RuleBasedRoutingSamplerBuilder builder =
        RuleBasedRoutingSampler.builder(SpanKind.SERVER, samplerConfiguration.fallback);
    samplerConfiguration.drop.forEach(d -> builder.drop(getHttpPathAttribute(), d));

    return builder.build();
  }

  private AttributeKey<String> getHttpPathAttribute() {
    return UrlAttributes.URL_PATH;
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
        logger.log(
            WARNING,
            "Failed to parse " + SAMPLER_ARG + " configuration option. Using default conf",
            ex);
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
