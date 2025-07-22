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

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.contrib.sampler.RuleBasedRoutingSampler;
import io.opentelemetry.contrib.sampler.RuleBasedRoutingSamplerBuilder;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.UrlAttributes;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nullable;

class RuleBasedSamplerFactory {
  private static final Logger logger = Logger.getLogger(RuleBasedSamplerFactory.class.getName());

  private RuleBasedSamplerFactory() {}

  public static RuleBasedRoutingSampler create(
      @Nullable String fallbackSamplerName, List<String> drop) {
    Sampler fallbackSampler = selectSampler(fallbackSamplerName);

    RuleBasedRoutingSamplerBuilder builder =
        RuleBasedRoutingSampler.builder(SpanKind.SERVER, fallbackSampler);
    drop.forEach(d -> builder.drop(UrlAttributes.URL_PATH, d));

    return builder.build();
  }

  private static Sampler selectSampler(@Nullable String samplerName) {
    if (samplerName != null) {
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
          logger.warning("Unsupported fallback sampler " + samplerName + ". Using default.");
      }
    }
    return Sampler.parentBased(Sampler.alwaysOn());
  }
}
