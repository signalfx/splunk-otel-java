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

package com.splunk.opentelemetry.micrometer;

import io.micrometer.signalfx.SignalFxConfig;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.time.Duration;

class SplunkMetricsConfig implements SignalFxConfig {
  @Override
  public boolean enabled() {
    return Config.get().getBooleanProperty("splunk.metrics.enabled", true);
  }

  @Override
  public String accessToken() {
    // non-empty token MUST be provided; we can just send anything because collector/SmartAgent will
    // use the real one
    return Config.get().getProperty("splunk.access.token", "no-token");
  }

  @Override
  public String uri() {
    // right now the default value points to SmartAgent endpoint
    return Config.get()
        .getProperty("splunk.metrics.endpoint", "http://localhost:9080/v2/datapoint");
  }

  @Override
  public String source() {
    return Resource.getDefault().getAttributes().get(ResourceAttributes.SERVICE_NAME);
  }

  @Override
  public Duration step() {
    long stepMillis =
        Long.parseLong(Config.get().getProperty("splunk.metrics.export.interval", "30000"));
    return Duration.ofMillis(stepMillis);
  }

  // hide other micrometer settings
  @Override
  public String prefix() {
    return "splunk.internal.metrics";
  }

  @Override
  public String get(String key) {
    return Config.get().getProperty(key);
  }
}
