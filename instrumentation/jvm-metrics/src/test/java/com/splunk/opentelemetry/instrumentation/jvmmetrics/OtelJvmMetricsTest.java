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

package com.splunk.opentelemetry.instrumentation.jvmmetrics;

import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.AbstractIterableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

@ExtendWith(AgentInstrumentationExtension.class)
class OtelJvmMetricsTest {
  private static final String INSTRUMENTATION_NAME = "com.splunk.javaagent.jvm-metrics-splunk";

  @RegisterExtension
  static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  @Test
  void shouldRegisterOtelJvmMeters() {
    // exercise the GC for a bit
    {
      List<Long[]> garbage = new ArrayList<>();
      for (int i = 0; i < 10_000; ++i) {
        garbage.add(new Long[i]);
      }
      garbage.clear();
    }
    System.gc();

    // thread metrics
    assertOtelMetricPresent("runtime.jvm.threads.states");
    // allocated memory metrics
    assertOtelMetricPresent(AllocatedMemoryMetrics.METRIC_NAME);
    // Our custom GC metrics
    assertOtelMetricPresent("runtime.jvm.gc.pause.count");
  }

  private void assertOtelMetricPresent(String name) {
    testing.waitAndAssertMetrics(INSTRUMENTATION_NAME, name, AbstractIterableAssert::isNotEmpty);
  }
}
