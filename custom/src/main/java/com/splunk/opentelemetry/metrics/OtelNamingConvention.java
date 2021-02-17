package com.splunk.opentelemetry.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.NamingConvention;

/**
 * This class just adds {@code runtime.} prefix to all JVM metrics. This is more of a demonstration
 * that we can rename/modify metrics rather that an attempt to follow any OTel conventions: there
 * are hardly any conventions for OTel metrics.
 */
class OtelNamingConvention implements NamingConvention {
  private final NamingConvention signalfxNamingConvention;

  OtelNamingConvention(NamingConvention signalfxNamingConvention) {
    this.signalfxNamingConvention = signalfxNamingConvention;
  }

  @Override
  public String name(String name, Meter.Type type, String baseUnit) {
    String metricName = signalfxNamingConvention.name(name, type, baseUnit);
    return metricName.startsWith("jvm.") ? "runtime." + metricName : metricName;
  }

  @Override
  public String tagKey(String key) {
    return signalfxNamingConvention.tagKey(key);
  }

  @Override
  public String tagValue(String value) {
    return signalfxNamingConvention.tagValue(value);
  }
}
