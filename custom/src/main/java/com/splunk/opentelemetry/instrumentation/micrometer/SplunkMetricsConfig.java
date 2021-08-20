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

package com.splunk.opentelemetry.instrumentation.micrometer;

import static com.splunk.opentelemetry.SplunkConfiguration.SPLUNK_ACCESS_TOKEN;

import io.micrometer.signalfx.SignalFxConfig;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.time.Duration;

class SplunkMetricsConfig implements SignalFxConfig {
  static final String METRICS_ENABLED_PROPERTY = "splunk.metrics.enabled";
  static final String METRICS_ENDPOINT_PROPERTY = "splunk.metrics.endpoint";
  static final String METRICS_EXPORT_INTERVAL_PROPERTY = "splunk.metrics.export.interval";

  // SmartAgent default endpoint: http://localhost:9080/v2/datapoint
  // OTel collector default endpoint: http://localhost:9943
  static final String DEFAULT_METRICS_ENDPOINT = "http://localhost:9943";
  private static final Duration DEFAULT_METRICS_EXPORT_INTERVAL = Duration.ofSeconds(30);

  private final Config config;
  // config values that are retrieved multiple times are cached
  private final String accessToken;
  private final String source;
  private final Duration step;

  SplunkMetricsConfig(Config config, Resource resource) {
    this.config = config;

    // non-empty token MUST be provided; we can just send anything because collector/SmartAgent will
    // use the real one
    accessToken = config.getProperty(SPLUNK_ACCESS_TOKEN, "no-token");
    source = resource.getAttributes().get(ResourceAttributes.SERVICE_NAME);
    step = config.getDuration(METRICS_EXPORT_INTERVAL_PROPERTY, DEFAULT_METRICS_EXPORT_INTERVAL);
  }

  @Override
  public boolean enabled() {
    return config.getBooleanProperty(METRICS_ENABLED_PROPERTY, false);
  }

  @Override
  public String accessToken() {
    return accessToken;
  }

  @Override
  public String uri() {
    return config.getProperty(METRICS_ENDPOINT_PROPERTY, DEFAULT_METRICS_ENDPOINT);
  }

  @Override
  public String source() {
    return source;
  }

  @Override
  public Duration step() {
    return step;
  }

  // hide other micrometer settings
  @Override
  public String prefix() {
    return "splunk.internal.metrics";
  }

  @Override
  public String get(String key) {
    return config.getProperty(key);
  }
}
