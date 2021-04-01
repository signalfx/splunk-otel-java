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

package com.splunk.opentelemetry.testing;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** This class is used in instrumentation tests; accessed via agent classloader bridging. */
@SuppressWarnings("unused")
public final class TestMetrics {
  public static Set<Map<String, Object>> getMeters() {
    return Metrics.globalRegistry.getMeters().stream()
        .map(TestMetrics::serializeMeter)
        .collect(toSet());
  }

  private static Map<String, Object> serializeMeter(Meter meter) {
    Map<String, Object> serialized = new HashMap<>();
    serialized.put("name", meter.getId().getName());
    serialized.put("unit", meter.getId().getBaseUnit());
    serialized.put("type", meter.getId().getType().name().toLowerCase());
    serialized.put(
        "tags", meter.getId().getTags().stream().collect(toMap(Tag::getKey, Tag::getValue)));
    return serialized;
  }

  public static void clearMetrics() {
    Metrics.globalRegistry.clear();
  }

  private TestMetrics() {}
}
