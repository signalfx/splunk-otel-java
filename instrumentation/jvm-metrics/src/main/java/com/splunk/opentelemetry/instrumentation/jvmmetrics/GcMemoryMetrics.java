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
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

public class GcMemoryMetrics implements AutoCloseable {
  private final boolean managementExtensionsPresent = isManagementExtensionsPresent();

  private final List<Runnable> notificationListenerCleanUpRunnables = new CopyOnWriteArrayList<>();

  public boolean isUnavailable() {
    return !managementExtensionsPresent;
  }

  public void registerListener(GcEventCallback gcEventCallback) {
    GcMetricsNotificationListener gcNotificationListener =
        new GcMetricsNotificationListener(gcEventCallback);
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
    private final GcEventCallback gcEventCallback;

    GcMetricsNotificationListener(GcEventCallback gcEventCallback) {
      this.gcEventCallback = gcEventCallback;
    }

    @Override
    public void handleNotification(Notification notification, Object ref) {
      CompositeData cd = (CompositeData) notification.getUserData();
      GarbageCollectionNotificationInfo notificationInfo =
          GarbageCollectionNotificationInfo.from(cd);

      if (gcEventCallback != null) {
        gcEventCallback.handleGcEvent(notificationInfo);
      }
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

  public interface GcEventCallback {
    void handleGcEvent(GarbageCollectionNotificationInfo notificationInfo);
  }
}
