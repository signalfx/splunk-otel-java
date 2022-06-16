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

import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_CALL_STACK_INTERVAL;

import io.opentelemetry.instrumentation.api.config.Config;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizes a configuration with user overrides. The config can contain
 * splunk.profiler.period.{short-event-name} keys whose values are the period in milliseconds,
 * without suffix.
 */
class JfrSettingsOverrides {

  private static final Logger logger = LoggerFactory.getLogger(JfrSettingsOverrides.class);
  private final Config config;

  JfrSettingsOverrides(Config config) {
    this.config = config;
  }

  Map<String, String> apply(Map<String, String> jfrSettings) {
    Map<String, String> settings = new HashMap<>(jfrSettings);
    Duration customInterval = getCustomInterval();
    if (customInterval != Duration.ZERO) {
      settings.put("jdk.ThreadDump#period", customInterval.toMillis() + " ms");
    }
    return maybeEnableTLABs(settings);
  }

  private Duration getCustomInterval() {
    Duration customInterval = config.getDuration(CONFIG_KEY_CALL_STACK_INTERVAL, Duration.ZERO);
    if (customInterval != Duration.ZERO) {
      return customInterval;
    }
    return Duration.ZERO;
  }

  private Map<String, String> maybeEnableTLABs(Map<String, String> settings) {
    if (Configuration.getTLABEnabled(config)) {
      settings.put("jdk.ObjectAllocationInNewTLAB#enabled", "true");
      settings.put("jdk.ObjectAllocationOutsideTLAB#enabled", "true");
    }
    return settings;
  }
}
