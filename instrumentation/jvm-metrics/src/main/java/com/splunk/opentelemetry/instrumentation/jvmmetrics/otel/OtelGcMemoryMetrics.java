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

import static com.splunk.opentelemetry.instrumentation.jvmmetrics.GcMemoryMetrics.METRIC_NAME;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import com.splunk.opentelemetry.instrumentation.jvmmetrics.GcMemoryMetrics;
import com.sun.management.GcInfo;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class OtelGcMemoryMetrics {

  private final boolean isGenerationalGc = isGenerationalGcConfigured();
  private final Set<String> longLivedPoolNames = new HashSet<>();
  private AtomicLong liveDataSize;

  public OtelGcMemoryMetrics() {
    for (MemoryPoolMXBean mbean : ManagementFactory.getMemoryPoolMXBeans()) {
      String name = mbean.getName();
      if (isLongLivedPool(name)) {
        longLivedPoolNames.add(name);
      }
    }
  }

  public void install() {
    GcMemoryMetrics gcMemoryMetrics = new GcMemoryMetrics();
    if (gcMemoryMetrics.isUnavailable()) {
      return;
    }

    Meter meter = OtelMeterProvider.get();
    meter
        .counterBuilder(METRIC_NAME)
        .setUnit("bytes")
        .setDescription("Sum of heap size differences before and after gc.")
        .buildWithCallback(measurement -> measurement.record(gcMemoryMetrics.getDeltaSum()));

    LongCounter gcPauseCounter =
        meter
            .counterBuilder("runtime.jvm.gc.pause.count")
            .setUnit("{gcs}")
            .setDescription("Number of gc pauses.")
            .build();
    LongCounter gcPauseTime =
        meter
            .counterBuilder("runtime.jvm.gc.pause.totalTime")
            .setUnit("ms")
            .setDescription("Time spent in GC pause.")
            .build();

    liveDataSize = new AtomicLong();
    meter
        .gaugeBuilder("runtime.jvm.gc.live.data.size")
        .setUnit("bytes")
        .setDescription("Size of long-lived heap memory pool after reclamation.")
        .buildWithCallback(measurement -> measurement.record(liveDataSize.get()));

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

          final Map<String, MemoryUsage> before = gcInfo.getMemoryUsageBeforeGc();
          final Map<String, MemoryUsage> after = gcInfo.getMemoryUsageAfterGc();

          long longLivedBefore =
              longLivedPoolNames.stream().mapToLong(pool -> before.get(pool).getUsed()).sum();
          long longLivedAfter =
              longLivedPoolNames.stream().mapToLong(pool -> after.get(pool).getUsed()).sum();

          // Some GC implementations such as G1 can reduce the old gen size as part of a minor GC.
          // To track the live data size we record the value if we see a reduction in the long-lived
          // heap size or after a major/non-generational GC.
          if (longLivedAfter < longLivedBefore
              || shouldUpdateDataSizeMetrics(notificationInfo.getGcName())) {
            liveDataSize.set(longLivedAfter);
          }
        });
  }

  private static boolean isConcurrentPhase(String cause, String name) {
    return "No GC".equals(cause)
        || "Shenandoah Cycles".equals(name)
        || "ZGC Cycles".equals(name)
        || (name.startsWith("GPGC") && !name.endsWith("Pauses"));
  }

  private static boolean isLongLivedPool(String name) {
    return name != null
        && (name.endsWith("Old Gen")
            || name.endsWith("Tenured Gen")
            || "Shenandoah".equals(name)
            || "ZHeap".equals(name)
            || name.endsWith("balanced-old")
            || name.contains("tenured") // "tenured",
            // "tenured-SOA",
            // "tenured-LOA"
            || "JavaHeap".equals(name) // metronome
        );
  }

  private boolean shouldUpdateDataSizeMetrics(String gcName) {
    return nonGenerationalGcShouldUpdateDataSize(gcName) || isMajorGenerationalGc(gcName);
  }

  private boolean isMajorGenerationalGc(String gcName) {
    return GcGenerationAge.fromGcName(gcName) == GcGenerationAge.OLD;
  }

  private boolean nonGenerationalGcShouldUpdateDataSize(String gcName) {
    return !isGenerationalGc
        // Skip Shenandoah and ZGC gc notifications with the name Pauses due
        // to missing memory pool size info
        && !gcName.endsWith("Pauses");
  }

  private boolean isGenerationalGcConfigured() {
    return ManagementFactory.getMemoryPoolMXBeans().stream()
            .filter(JvmMemory::isHeap)
            .map(MemoryPoolMXBean::getName)
            .filter(name -> !name.contains("tenured"))
            .count()
        > 1;
  }

  /**
   * Generalization of which parts of the heap are considered "young" or "old" for multiple GC
   * implementations
   */
  enum GcGenerationAge {
    OLD,
    YOUNG,
    UNKNOWN;

    private static final Map<String, GcGenerationAge> knownCollectors =
        new HashMap<String, GcGenerationAge>() {
          {
            put("ConcurrentMarkSweep", OLD);
            put("Copy", YOUNG);
            put("G1 Old Generation", OLD);
            put("G1 Young Generation", YOUNG);
            put("MarkSweepCompact", OLD);
            put("PS MarkSweep", OLD);
            put("PS Scavenge", YOUNG);
            put("ParNew", YOUNG);
            put("global", OLD);
            put("scavenge", YOUNG);
            put("partial gc", YOUNG);
            put("global garbage collect", OLD);
            put("Epsilon", OLD);
          }
        };

    static GcGenerationAge fromGcName(String gcName) {
      return knownCollectors.getOrDefault(gcName, UNKNOWN);
    }
  }
}
