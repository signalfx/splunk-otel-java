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

import com.splunk.opentelemetry.instrumentation.jvmmetrics.GcMemoryMetrics;
import com.sun.management.GcInfo;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

public class OtelGcMemoryMetrics {

  public void install() {
    GcMemoryMetrics gcMemoryMetrics = new GcMemoryMetrics();
    if (gcMemoryMetrics.isUnavailable()) {
      return;
    }

    Meter meter = OtelMeterProvider.get();
    LongCounter gcPauseCounter =
        meter
            .counterBuilder("jvm.gc.pause.count")
            .setUnit("{gcs}")
            .setDescription("Number of gc pauses. This metric will be removed in a future release.")
            .build();
    LongCounter gcPauseTime =
        meter
            .counterBuilder("jvm.gc.pause.totalTime")
            .setUnit("ms")
            .setDescription(
                "Time spent in GC pause. This metric will be removed in a future release.")
            .build();

    gcMemoryMetrics.registerListener(
        notificationInfo -> {
          GcInfo gcInfo = notificationInfo.getGcInfo();
          String gcName = notificationInfo.getGcName();
          String gcCause = notificationInfo.getGcCause();
          String gcAction = notificationInfo.getGcAction();
          long duration = gcInfo.getDuration();
          if (!isConcurrentPhase(gcCause, gcName)) {
            Attributes attributes =
                Attributes.of(
                    stringKey("gc"),
                    gcName,
                    stringKey("action"),
                    gcAction,
                    stringKey("cause"),
                    gcCause);
            gcPauseCounter.add(1, attributes);
            gcPauseTime.add(duration, attributes);
          }
        });
  }

  private static boolean isConcurrentPhase(String cause, String name) {
    return "No GC".equals(cause)
        || "Shenandoah Cycles".equals(name)
        || "ZGC Cycles".equals(name)
        || (name.startsWith("GPGC") && !name.endsWith("Pauses"));
  }
}
