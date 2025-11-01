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

package com.splunk.opentelemetry.profiler;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Customizes a configuration with user overrides. The config can contain
 * splunk.profiler.period.{short-event-name} keys whose values are the period in milliseconds,
 * without suffix.
 */
class JfrSettingsOverrides {

  private final ConfigProperties config;

  JfrSettingsOverrides(ConfigProperties config) {
    this.config = config;
  }

  Map<String, String> apply(Map<String, String> jfrSettings) {
    Map<String, String> settings = new HashMap<>(jfrSettings);
    Duration customInterval = Configuration.getCallStackInterval(config);
    if (!Duration.ZERO.equals(customInterval)) {
      settings.put("jdk.ThreadDump#period", customInterval.toMillis() + " ms");
    }
    return maybeEnableTLABs(settings);
  }

  private Map<String, String> maybeEnableTLABs(Map<String, String> settings) {
    if (Configuration.getMemoryEnabled(config)) {
      if (Configuration.getMemoryEventRateLimitEnabled(config)
          && Configuration.getUseAllocationSampleEvent(config)) {
        settings.put("jdk.ObjectAllocationSample#enabled", "true");
        settings.put(
            "jdk.ObjectAllocationSample#throttle", Configuration.getMemoryEventRate(config));
      } else {
        settings.put("jdk.ObjectAllocationInNewTLAB#enabled", "true");
        settings.put("jdk.ObjectAllocationOutsideTLAB#enabled", "true");
      }
    }
    return settings;
  }
}
