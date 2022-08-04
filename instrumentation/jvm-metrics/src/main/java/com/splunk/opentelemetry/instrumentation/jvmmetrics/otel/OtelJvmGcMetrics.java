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

import static com.splunk.opentelemetry.instrumentation.jvmmetrics.otel.JvmMemory.getLongLivedHeapPools;
import static com.splunk.opentelemetry.instrumentation.jvmmetrics.otel.JvmMemory.getUsageValue;
import static com.splunk.opentelemetry.instrumentation.jvmmetrics.otel.JvmMemory.isAllocationPool;
import static com.splunk.opentelemetry.instrumentation.jvmmetrics.otel.JvmMemory.isConcurrentPhase;
import static com.splunk.opentelemetry.instrumentation.jvmmetrics.otel.JvmMemory.isLongLivedPool;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OtelJvmGcMetrics {
  private static final Logger logger = LoggerFactory.getLogger(OtelJvmGcMetrics.class);

  private static final AttributeKey<String> ACTION = stringKey("action");
  private static final AttributeKey<String> CAUSE = stringKey("cause");

  private final boolean managementExtensionsPresent = isManagementExtensionsPresent();
  private final boolean isGenerationalGc = isGenerationalGcConfigured();
  private String allocationPoolName;
  private final Set<String> longLivedPoolNames = new HashSet<>();

  private LongCounter allocatedBytes;
  private LongCounter promotedBytes;
  private AtomicLong allocationPoolSizeAfter;
  private AtomicLong liveDataSize;
  private AtomicLong maxDataSize;

  public OtelJvmGcMetrics() {
    for (MemoryPoolMXBean mbean : ManagementFactory.getMemoryPoolMXBeans()) {
      String name = mbean.getName();
      if (isAllocationPool(name)) {
        allocationPoolName = name;
      }
      if (isLongLivedPool(name)) {
        longLivedPoolNames.add(name);
      }
    }
  }

  public void install() {
    if (!this.managementExtensionsPresent) {
      return;
    }

    Meter meter = OtelMeterProvider.get();

    GcMetricsNotificationListener gcNotificationListener = new GcMetricsNotificationListener(meter);

    double maxLongLivedPoolBytes =
        getLongLivedHeapPools().mapToDouble(mem -> getUsageValue(mem, MemoryUsage::getMax)).sum();

    maxDataSize = new AtomicLong((long) maxLongLivedPoolBytes);
    meter
        .gaugeBuilder("runtime.jvm.gc.max.data.size")
        .setUnit("By")
        .setDescription("Max size of long-lived heap memory pool.")
        .buildWithCallback(measurement -> measurement.record(maxDataSize.get()));

    liveDataSize = new AtomicLong();
    meter
        .gaugeBuilder("runtime.jvm.gc.live.data.size")
        .setUnit("By")
        .setDescription("Size of long-lived heap memory pool after reclamation.")
        .buildWithCallback(measurement -> measurement.record(liveDataSize.get()));

    allocatedBytes =
        meter
            .counterBuilder("runtime.jvm.gc.memory.allocated")
            .setUnit("By")
            .setDescription("Size of long-lived heap memory pool after reclamation.")
            .build();

    if (isGenerationalGc) {
      promotedBytes =
          meter
              .counterBuilder("runtime.jvm.gc.memory.promoted")
              .setUnit("By")
              .setDescription(
                  "Count of positive increases in the size of the old generation memory pool before GC to after GC.")
              .build();
    }

    allocationPoolSizeAfter = new AtomicLong(0L);

    for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
      if (!(gcBean instanceof NotificationEmitter)) {
        continue;
      }
      NotificationEmitter notificationEmitter = (NotificationEmitter) gcBean;
      notificationEmitter.addNotificationListener(
          gcNotificationListener,
          notification ->
              notification
                  .getType()
                  .equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION),
          null);
    }
  }

  private class GcMetricsNotificationListener implements NotificationListener {

    private final LongHistogram concurrentPhaseHistogram;
    private final LongHistogram pauseHistogram;

    GcMetricsNotificationListener(Meter meter) {
      concurrentPhaseHistogram =
          meter
              .histogramBuilder("runtime.jvm.gc.concurrent.phase.time")
              .ofLongs()
              .setUnit("ms")
              .setDescription("Time spent in concurrent phase.")
              .build();
      pauseHistogram =
          meter
              .histogramBuilder("runtime.jvm.gc.pause")
              .ofLongs()
              .setUnit("ms")
              .setDescription("Time spent in GC pause.")
              .build();
    }

    @Override
    public void handleNotification(Notification notification, Object ref) {
      CompositeData cd = (CompositeData) notification.getUserData();
      GarbageCollectionNotificationInfo notificationInfo =
          GarbageCollectionNotificationInfo.from(cd);

      String gcCause = notificationInfo.getGcCause();
      String gcAction = notificationInfo.getGcAction();
      GcInfo gcInfo = notificationInfo.getGcInfo();
      long duration = gcInfo.getDuration();
      Attributes attributes = Attributes.of(ACTION, gcAction, CAUSE, gcCause);
      if (isConcurrentPhase(gcCause, notificationInfo.getGcName())) {
        concurrentPhaseHistogram.record(duration, attributes);
      } else {
        pauseHistogram.record(duration, attributes);
      }

      final Map<String, MemoryUsage> before = gcInfo.getMemoryUsageBeforeGc();
      final Map<String, MemoryUsage> after = gcInfo.getMemoryUsageAfterGc();

      countPoolSizeDelta(before, after);

      final long longLivedBefore =
          longLivedPoolNames.stream().mapToLong(pool -> before.get(pool).getUsed()).sum();
      final long longLivedAfter =
          longLivedPoolNames.stream().mapToLong(pool -> after.get(pool).getUsed()).sum();
      if (isGenerationalGc) {
        final long delta = longLivedAfter - longLivedBefore;
        if (delta > 0L) {
          promotedBytes.add(delta);
        }
      }

      // Some GC implementations such as G1 can reduce the old gen size as part of a minor GC. To
      // track the live data size we record the value if we see a reduction in the long-lived heap
      // size or after a major/non-generational GC.
      if (longLivedAfter < longLivedBefore
          || shouldUpdateDataSizeMetrics(notificationInfo.getGcName())) {
        liveDataSize.set(longLivedAfter);
        maxDataSize.set(
            longLivedPoolNames.stream().mapToLong(pool -> after.get(pool).getMax()).sum());
      }
    }

    private void countPoolSizeDelta(
        Map<String, MemoryUsage> before, Map<String, MemoryUsage> after) {
      if (allocationPoolName == null) {
        return;
      }
      final long beforeBytes = before.get(allocationPoolName).getUsed();
      final long afterBytes = after.get(allocationPoolName).getUsed();
      final long delta = beforeBytes - allocationPoolSizeAfter.get();
      allocationPoolSizeAfter.set(afterBytes);
      if (delta > 0L) {
        allocatedBytes.add(delta);
      }
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
  }

  private boolean isGenerationalGcConfigured() {
    return ManagementFactory.getMemoryPoolMXBeans().stream()
            .filter(JvmMemory::isHeap)
            .map(MemoryPoolMXBean::getName)
            .filter(name -> !name.contains("tenured"))
            .count()
        > 1;
  }

  private static boolean isManagementExtensionsPresent() {
    if (ManagementFactory.getMemoryPoolMXBeans().isEmpty()) {
      // Substrate VM, for example, doesn't provide or support these beans (yet)
      logger.warn(
          "GC notifications will not be available because MemoryPoolMXBeans are not provided by the JVM");
      return false;
    }

    try {
      Class.forName(
          "com.sun.management.GarbageCollectionNotificationInfo",
          false,
          MemoryPoolMXBean.class.getClassLoader());
      return true;
    } catch (Throwable e) {
      // We are operating in a JVM without access to this level of detail
      logger.warn(
          "GC notifications will not be available because "
              + "com.sun.management.GarbageCollectionNotificationInfo is not present");
      return false;
    }
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
