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

package com.splunk.opentelemetry.jvmmetrics;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.splunk.opentelemetry.testing.MeterData;
import com.splunk.opentelemetry.testing.TestMetricsAccess;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(AgentInstrumentationExtension.class)
class JvmMetricsTest {
  @Test
  void shouldRegisterJvmMeters() {
    var meters = TestMetricsAccess.getMeters().stream().map(MeterData::getName).collect(toSet());

    // classloader metrics
    assertTrue(meters.contains("jvm.classes.loaded"));
    // GC metrics
    assertTrue(meters.contains("jvm.gc.memory.allocated"));
    // memory metrics
    assertTrue(meters.contains("jvm.memory.used"));
    // thread metrics
    assertTrue(meters.contains("jvm.threads.peak"));
  }
}
