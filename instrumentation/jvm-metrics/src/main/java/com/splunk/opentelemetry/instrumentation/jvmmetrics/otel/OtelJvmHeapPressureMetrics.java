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

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;
import io.opentelemetry.api.metrics.Meter;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

public class OtelJvmHeapPressureMetrics {
  private final long startOfMonitoring = System.nanoTime();
  private final Duration lookback;
  private final TimeWindowSum gcPauseSum;

  public OtelJvmHeapPressureMetrics() {
    this(Duration.ofMinutes(5), Duration.ofMinutes(1));
  }

  private OtelJvmHeapPressureMetrics(Duration lookback, Duration testEvery) {
    this.lookback = lookback;
    this.gcPauseSum =
        new TimeWindowSum((int) lookback.dividedBy(testEvery.toMillis()).toMillis(), testEvery);

    monitor();
  }

  public void install() {
    Meter meter = OtelMeterProvider.get();

    // runtime.jvm.memory.usage.after.gc is replaced by OTel
    //   process.runtime.jvm.memory.usage_after_last_gc{pool=<long lived pools>,type=heap} /
    //   process.runtime.jvm.memory.limit{pool=<long lived pools>,type=heap}

    meter
        .gaugeBuilder("runtime.jvm.gc.overhead")
        .setUnit("percent")
        .setDescription(
            "An approximation of the percent of CPU time used by GC activities over the last lookback period or since monitoring began, whichever is shorter, in the range [0..1].")
        .buildWithCallback(
            measurement -> {
              double overIntervalMillis =
                  Math.min(System.nanoTime() - startOfMonitoring, lookback.toNanos()) / 1e6;
              measurement.record(gcPauseSum.poll() / overIntervalMillis);
            });
  }

  private void monitor() {
    for (GarbageCollectorMXBean mbean : ManagementFactory.getGarbageCollectorMXBeans()) {
      if (!(mbean instanceof NotificationEmitter)) {
        continue;
      }
      NotificationListener notificationListener =
          (notification, ref) -> {
            CompositeData cd = (CompositeData) notification.getUserData();
            GarbageCollectionNotificationInfo notificationInfo =
                GarbageCollectionNotificationInfo.from(cd);

            String gcCause = notificationInfo.getGcCause();
            GcInfo gcInfo = notificationInfo.getGcInfo();
            long duration = gcInfo.getDuration();

            if (!JvmMemory.isConcurrentPhase(gcCause, notificationInfo.getGcName())) {
              gcPauseSum.record(duration);
            }
          };
      NotificationEmitter notificationEmitter = (NotificationEmitter) mbean;
      notificationEmitter.addNotificationListener(
          notificationListener,
          notification ->
              notification
                  .getType()
                  .equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION),
          null);
    }
  }
}
