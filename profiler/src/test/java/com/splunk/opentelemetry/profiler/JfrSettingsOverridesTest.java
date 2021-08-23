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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.instrumentation.api.config.Config;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JfrSettingsOverridesTest {

  @Test
  void testOverrides() {
    Config config = mock(Config.class);
    when(config.getString("splunk.profiler.period.threaddump")).thenReturn("163");
    when(config.getString("splunk.profiler.period.otherevent")).thenReturn("964");
    when(config.getString("splunk.profiler.period.extraunused")).thenReturn("111");
    when(config.getBoolean("splunk.profiler.tlab.enabled", false)).thenReturn(true);
    JfrSettingsOverrides overrides = new JfrSettingsOverrides(config);
    Map<String, String> jfrSettings =
        Map.of(
            "jdk.ThreadDump#period", "12",
            "jdk.ThreadDump#enabled", "true",
            "jdk.OtherEvent#period", "13",
            "jdk.ObjectAllocationInNewTLAB#enabled", "true",
            "jdk.ObjectAllocationOutsideTLAB#enabled", "true");
    Map<String, String> result = overrides.apply(jfrSettings);
    assertNotSame(result, jfrSettings);
    assertEquals("163 ms", result.get("jdk.ThreadDump#period"));
    assertEquals("964 ms", result.get("jdk.OtherEvent#period"));
    assertEquals("true", result.get("jdk.ObjectAllocationInNewTLAB#enabled"));
    assertEquals("true", result.get("jdk.ObjectAllocationOutsideTLAB#enabled"));
  }
}
