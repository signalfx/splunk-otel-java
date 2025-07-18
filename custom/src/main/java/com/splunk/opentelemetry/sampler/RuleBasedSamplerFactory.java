package com.splunk.opentelemetry.sampler;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.contrib.sampler.RuleBasedRoutingSampler;
import io.opentelemetry.contrib.sampler.RuleBasedRoutingSamplerBuilder;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.UrlAttributes;
import javax.annotation.Nullable;
import java.util.List;
import java.util.logging.Logger;

class RuleBasedSamplerFactory {
  private static final Logger logger = Logger.getLogger(RuleBasedSamplerFactory.class.getName());

  private RuleBasedSamplerFactory() {}

  public static RuleBasedRoutingSampler create(@Nullable String fallbackSamplerName, List<String> drop) {
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
