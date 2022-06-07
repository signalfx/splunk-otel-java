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

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

public class GcMemoryMetrics implements MeterBinder, AutoCloseable {
  public static final String METRIC_NAME = "process.runtime.jvm.memory.reclaimed";
  private final boolean managementExtensionsPresent = isManagementExtensionsPresent();

  private final List<Runnable> notificationListenerCleanUpRunnables = new CopyOnWriteArrayList<>();
  private final Set<String> heapPoolNames = new HashSet<>();
  private final AtomicLong deltaSum = new AtomicLong();

  @Override
  public void bindTo(MeterRegistry registry) {
    if (!this.managementExtensionsPresent) {
      return;
    }

    GcMetricsNotificationListener gcNotificationListener =
        new GcMetricsNotificationListener(registry);

    Gauge.builder(METRIC_NAME, deltaSum, AtomicLong::get)
        .description("Sum of heap size differences before and after gc")
        .baseUnit(BaseUnits.BYTES)
        .register(registry);

    ManagementFactory.getMemoryPoolMXBeans().stream()
        .filter(pool -> MemoryType.HEAP.equals(pool.getType()))
        .map(MemoryPoolMXBean::getName)
        .forEach(heapPoolNames::add);

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
      notificationListenerCleanUpRunnables.add(
          () -> {
            try {
              notificationEmitter.removeNotificationListener(gcNotificationListener);
            } catch (ListenerNotFoundException ignore) {
            }
          });
    }
  }

  @Override
  public void close() {
    notificationListenerCleanUpRunnables.forEach(Runnable::run);
  }

  class GcMetricsNotificationListener implements NotificationListener {
    private final MeterRegistry registry;

    GcMetricsNotificationListener(MeterRegistry registry) {
      this.registry = registry;
    }

    @Override
    public void handleNotification(Notification notification, Object ref) {
      CompositeData cd = (CompositeData) notification.getUserData();
      GarbageCollectionNotificationInfo notificationInfo =
          GarbageCollectionNotificationInfo.from(cd);

      GcInfo gcInfo = notificationInfo.getGcInfo();
      final Map<String, MemoryUsage> before = gcInfo.getMemoryUsageBeforeGc();
      final Map<String, MemoryUsage> after = gcInfo.getMemoryUsageAfterGc();

      long usageBefore = sumMemoryUsage(before);
      long usageAfter = sumMemoryUsage(after);

      deltaSum.addAndGet(usageBefore - usageAfter);
    }

    private long sumMemoryUsage(Map<String, MemoryUsage> memoryUsageMap) {
      long result = 0;
      for (Map.Entry<String, MemoryUsage> entry : memoryUsageMap.entrySet()) {
        String poolName = entry.getKey();
        MemoryUsage memoryUsage = entry.getValue();
        if (!heapPoolNames.contains(poolName)) {
          continue;
        }

        result += memoryUsage.getUsed();
      }
      return result;
    }
  }

  // copied from micrometer JvmGcMetrics
  private static boolean isManagementExtensionsPresent() {
    if (ManagementFactory.getMemoryPoolMXBeans().isEmpty()) {
      // Substrate VM, for example, doesn't provide or support these beans (yet)
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
      return false;
    }
  }
}
