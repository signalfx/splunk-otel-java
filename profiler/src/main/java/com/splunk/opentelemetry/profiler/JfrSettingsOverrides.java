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

import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_PERIOD_PREFIX;
import static com.splunk.opentelemetry.profiler.Configuration.CONFIG_KEY_TLAB_ENABLED;

import io.opentelemetry.instrumentation.api.config.Config;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizes a configuration with user overrides. The config can contain
 * splunk.profiler.period.{short-event-name} keys whose valuse are the period in milliseconds,
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
    jfrSettings.keySet().stream()
        .filter(key -> key.endsWith("#period"))
        .forEach(
            key -> {
              String[] parts = key.split("#");
              String eventName = parts[0];
              String shortEventName = eventName.replaceFirst("^jdk.", "");
              String configKey = CONFIG_KEY_PERIOD_PREFIX + "." + shortEventName.toLowerCase();
              String customSetting = config.getProperty(configKey);
              if (customSetting != null) {
                String jfrFormattedDuration = customSetting + " ms";
                logger.info(
                    "Custom JFR period configured for {} :: {}", eventName, jfrFormattedDuration);
                settings.put(key, jfrFormattedDuration);
              }
            });
    return maybeEnableTLABs(settings);
  }

  private Map<String, String> maybeEnableTLABs(Map<String, String> settings) {
    if (config.getBooleanProperty(CONFIG_KEY_TLAB_ENABLED, false)) {
      settings.put("jdk.ObjectAllocationInNewTLAB#enabled", "true");
      settings.put("jdk.ObjectAllocationOutsideTLAB#enabled", "true");
    }
    return settings;
  }
}
