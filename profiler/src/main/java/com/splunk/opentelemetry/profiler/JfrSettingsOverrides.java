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

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.IntStream;

/**
 * Customizes a configuration with user overrides. The config can contain
 * splunk.profiler.period.{short-event-name} keys whose values are the period in milliseconds,
 * without suffix.
 */
class JfrSettingsOverrides {

  private static final Logger logger = Logger.getLogger(JfrSettingsOverrides.class.getName());

  private final ConfigProperties config;

  JfrSettingsOverrides(ConfigProperties config) {
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

    if(isRunningVersionUnsafeForJfr()){
      logger.warning("*****************************************************************");
      logger.warning("**************************** WARNING ****************************");
      logger.warning("* JDK " + jdkVersion() + " is vulnerable to JDK-8309862. Memory ");
      logger.warning("* profiling will not be enabled. You will need to               ");
      logger.warning("* upgrade the JDK to use the memory profiler.                   ");
      logger.warning("*****************************************************************");
      logger.warning("*****************************************************************");
      return settings;
    }

    if (Configuration.getTLABEnabled(config)) {
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

  private static boolean isRunningVersionUnsafeForJfr(){
    String version = jdkVersion();
    return isVulnerableToJdk8309862(version);
  }

  @VisibleForTesting
  static boolean isVulnerableToJdk8309862(String version){
    if(version == null){
      return false;
    }
    return IntStream.range(0, 9)
        .mapToObj(i -> "^17\\.0\\." + i + "(\\..*|$)")
        .anyMatch(version::matches);
  }

  private static String jdkVersion() {
    String version = System.getProperty("java.runtime.version");
    return version;
  }

}
