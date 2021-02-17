package com.splunk.opentelemetry.metrics;

import io.micrometer.signalfx.SignalFxConfig;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.time.Duration;

// TODO: move some defaults to SplunkConfiguration -- or maybe not?
class SplunkMetricsConfig implements SignalFxConfig {
  @Override
  public boolean enabled() {
    return Config.get().getBooleanProperty("splunk.metrics.enabled", false);
  }

  @Override
  public String accessToken() {
    // token can be configured in the SmartAgent or otel-collector instead - probably
    // TODO: rename property to splunk.access.token - splunk-otel-collector uses
    // SPLUNK_ACCESS_TOKEN env var, let's be consistent
    return Config.get().getProperty("signalfx.auth.token", "no-token");
  }

  @Override
  public String uri() {
    return Config.get()
        .getProperty("splunk.metrics.endpoint", "http://localhost:9080/v2/datapoint");
  }

  @Override
  public String source() {
    return Resource.getDefault().getAttributes().get(ResourceAttributes.SERVICE_NAME);
  }

//  @Override
//  public Duration step() {
//    // TODO: should be configurable
//    return Duration.ofSeconds(30);
//  }

  // hide other micrometer settings
  @Override
  public String prefix() {
    return "splunk.metrics.internal";
  }

  @Override
  public String get(String key) {
    return Config.get().getProperty(key);
  }
}
