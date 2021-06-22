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

package com.splunk.opentelemetry.profiler.events;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class EventPeriods {

  public static final Duration UNKNOWN = Duration.ZERO;
  private final Map<String, Duration> cache = new HashMap<>();
  private final Function<String, String> configFinder;

  public EventPeriods(Function<String, String> configFinder) {
    this.configFinder = configFinder;
  }

  public Duration getDuration(String eventName) {
    return cache.computeIfAbsent(
        eventName,
        event -> {
          String value = configFinder.apply(event + "#period");
          return parseToDuration(value);
        });
  }

  private Duration parseToDuration(String value) {
    if (value == null) {
      return UNKNOWN;
    }
    // format is "TTT UUU" where TTT is some numbers and UUU is some units suffix (ms or s)
    try {
      String[] parts = value.split(" ");
      if (parts.length < 2) {
        return UNKNOWN;
      }
      long multiplier = 1;
      if ("s".equals(parts[1])) {
        multiplier = 1000;
      }
      return Duration.ofMillis(multiplier * Integer.parseInt(parts[0]));
    } catch (NumberFormatException e) {
      return UNKNOWN;
    }
  }
}
