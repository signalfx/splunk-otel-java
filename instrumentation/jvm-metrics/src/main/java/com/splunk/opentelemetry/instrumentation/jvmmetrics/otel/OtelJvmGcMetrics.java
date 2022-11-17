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

import static com.splunk.opentelemetry.instrumentation.jvmmetrics.otel.JvmMemory.isAllocationPool;
import static com.splunk.opentelemetry.instrumentation.jvmmetrics.otel.JvmMemory.isLongLivedPool;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

public class OtelJvmGcMetrics {
  private static final Logger logger = Logger.getLogger(OtelJvmGcMetrics.class.getName());

  private final boolean managementExtensionsPresent = isManagementExtensionsPresent();
  private final boolean isGenerationalGc = isGenerationalGcConfigured();
  private String allocationPoolName;
  private final Set<String> longLivedPoolNames = new HashSet<>();

  private LongCounter allocatedBytes;
  private LongCounter promotedBytes;
  private AtomicLong allocationPoolSizeAfter;

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

    GcMetricsNotificationListener gcNotificationListener = new GcMetricsNotificationListener();

    // runtime.jvm.gc.max.data.size is replaced by OTel
    //   process.runtime.jvm.memory.limit{pool=<long lived pools>}

    // runtime.jvm.gc.live.data.size is replaced by OTel
    //   process.runtime.jvm.memory.usage_after_last_gc{pool=<long lived pools>}

    allocatedBytes =
        meter
            .counterBuilder("runtime.jvm.gc.memory.allocated")
            .setUnit("bytes")
            .setDescription("Size of long-lived heap memory pool after reclamation.")
            .build();

    if (isGenerationalGc) {
      promotedBytes =
          meter
              .counterBuilder("runtime.jvm.gc.memory.promoted")
              .setUnit("bytes")
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

    // runtime.jvm.gc.concurrent.phase.time is replaced by OTel
    //   process.runtime.jvm.gc.duration{gc=<concurrent gcs>}
    // runtime.jvm.gc.pause is replaced by OTel
    //   process.runtime.jvm.gc.duration{gc!=<concurrent gcs>}

    @Override
    public void handleNotification(Notification notification, Object ref) {
      CompositeData cd = (CompositeData) notification.getUserData();
      GarbageCollectionNotificationInfo notificationInfo =
          GarbageCollectionNotificationInfo.from(cd);

      GcInfo gcInfo = notificationInfo.getGcInfo();
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
      logger.warning(
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
      logger.warning(
          "GC notifications will not be available because "
              + "com.sun.management.GarbageCollectionNotificationInfo is not present");
      return false;
    }
  }
}
