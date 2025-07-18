package com.splunk.opentelemetry.sampler;

import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;

class RuleBasedSamplerConfig {
  private static final Logger logger = Logger.getLogger(RuleBasedSamplerConfig.class.getName());

  final List<String> drop = new ArrayList<>();
  Sampler fallback = Sampler.parentBased(Sampler.alwaysOn());

  private RuleBasedSamplerConfig() {}

  public static RuleBasedSamplerConfig parse(String configString) {
    RuleBasedSamplerConfig config = new RuleBasedSamplerConfig();
    if (configString == null) {
      return config;
    }

    try {
      String[] parts = configString.split(";");
      for (String part : parts) {
        if (part.startsWith("fallback")) {
          config.fallback = selectSampler(part.split("=")[1]);
        } else if (part.startsWith("drop")) {
          config.drop.add(part.split("=")[1]);
        }
      }
    } catch (RuntimeException ex) {
      logger.log(
          WARNING,
          "Failed to parse sampler configuration string. Using default config",
          ex);
    }
    return config;
  }

  @Override
  public String toString() {
    return "Config{" + "drop=" + drop + ", fallback=" + fallback + '}';
  }

  private static Sampler selectSampler(String samplerName) {
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

