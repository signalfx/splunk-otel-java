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

// Includes work from:
/*
 * Copyright 2019 VMware, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.opentelemetry.instrumentation.jvmmetrics.otel;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Locale;

public class OtelJvmThreadMetrics {
  private static final AttributeKey<String> STATE = stringKey("state");

  public void install() {
    Meter meter = OtelMeterProvider.get();
    ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    meter
        .gaugeBuilder("runtime.jvm.threads.peak")
        .setUnit("threads")
        .setDescription(
            "The peak live thread count since the Java virtual machine started or peak was reset.")
        .buildWithCallback(measurement -> measurement.record(threadBean.getPeakThreadCount()));

    meter
        .gaugeBuilder("runtime.jvm.threads.daemon")
        .setUnit("threads")
        .setDescription("The current number of live daemon threads.")
        .buildWithCallback(measurement -> measurement.record(threadBean.getDaemonThreadCount()));

    meter
        .gaugeBuilder("runtime.jvm.threads.live")
        .setUnit("threads")
        .setDescription(
            "The current number of live threads including both daemon and non-daemon threads.")
        .buildWithCallback(measurement -> measurement.record(threadBean.getThreadCount()));

    try {
      threadBean.getAllThreadIds();
      for (Thread.State state : Thread.State.values()) {
        Attributes attributes = Attributes.of(STATE, getStateTagValue(state));

        meter
            .gaugeBuilder("runtime.jvm.threads.states")
            .setUnit("threads")
            .setDescription(
                "The current number of threads that are currently in state described by the state attribute.")
            .buildWithCallback(
                measurement ->
                    measurement.record(getThreadStateCount(threadBean, state), attributes));
      }
    } catch (Error error) {
      // An error will be thrown for unsupported operations
      // e.g. SubstrateVM does not support getAllThreadIds
    }
  }

  private static long getThreadStateCount(ThreadMXBean threadBean, Thread.State state) {
    return Arrays.stream(threadBean.getThreadInfo(threadBean.getAllThreadIds()))
        .filter(threadInfo -> threadInfo != null && threadInfo.getThreadState() == state)
        .count();
  }

  private static String getStateTagValue(Thread.State state) {
    return state.name().toLowerCase(Locale.ROOT).replace("_", "-");
  }
}
