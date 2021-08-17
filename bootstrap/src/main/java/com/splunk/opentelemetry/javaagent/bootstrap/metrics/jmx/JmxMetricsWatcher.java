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

package com.splunk.opentelemetry.javaagent.bootstrap.metrics.jmx;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JmxMetricsWatcher {

  private static final Logger log = LoggerFactory.getLogger(JmxMetricsWatcher.class);

  private static final AtomicInteger threadIds = new AtomicInteger();
  private static final ThreadFactory daemonThreadFactory =
      r -> {
        Thread t = new Thread(r, "JmxMetricsWatcher-" + threadIds.incrementAndGet());
        t.setDaemon(true);
        t.setContextClassLoader(null);
        return t;
      };

  private static final Set<String> handledNotificationTypes =
      new HashSet<>(
          asList(
              MBeanServerNotification.REGISTRATION_NOTIFICATION,
              MBeanServerNotification.UNREGISTRATION_NOTIFICATION));

  private final MBeanServer mBeanServer;
  private final ExecutorService executorService;
  private final MeterRegistry meterRegistry;
  private final JmxQuery query;
  private final MetersFactory metersFactory;

  private final Map<ObjectName, List<Meter>> meters = new ConcurrentHashMap<>();
  private volatile NotificationListener notificationListener;

  public JmxMetricsWatcher(JmxQuery query, MetersFactory metersFactory) {
    this(
        findMBeanServer(),
        Executors.newSingleThreadExecutor(daemonThreadFactory),
        Metrics.globalRegistry,
        query,
        metersFactory);
  }

  // visible for tests
  JmxMetricsWatcher(
      MBeanServer mBeanServer,
      ExecutorService executorService,
      MeterRegistry meterRegistry,
      JmxQuery query,
      MetersFactory metersFactory) {
    this.mBeanServer = mBeanServer;
    this.executorService = executorService;
    this.meterRegistry = meterRegistry;
    this.query = query;
    this.metersFactory = metersFactory;
  }

  // copied from micrometer CommonsObjectPool2Metrics class
  private static MBeanServer findMBeanServer() {
    List<MBeanServer> mBeanServers = MBeanServerFactory.findMBeanServer(null);
    if (!mBeanServers.isEmpty()) {
      return mBeanServers.get(0);
    }
    return ManagementFactory.getPlatformMBeanServer();
  }

  public void start() {
    try {
      // get all mbeans matching the query, register meters for them
      Set<ObjectName> existingObjects = mBeanServer.queryNames(query.toObjectNameQuery(), null);
      for (ObjectName existingObject : existingObjects) {
        meters.compute(existingObject, this::computeMeters);
      }

      // register a notification listener that'll add and remove meters as they come
      NotificationFilter filter = this::isNotificationEnabled;
      notificationListener = this::handleNotification;
      mBeanServer.addNotificationListener(
          MBeanServerDelegate.DELEGATE_NAME, notificationListener, filter, null);
    } catch (JMException e) {
      log.warn("Could not start JMX metrics listener", e);
    }
  }

  public void stop() {
    if (notificationListener != null) {
      try {
        mBeanServer.removeNotificationListener(
            MBeanServerDelegate.DELEGATE_NAME, notificationListener);
        notificationListener = null;
      } catch (InstanceNotFoundException | ListenerNotFoundException e) {
        log.warn("Could not remove JMX metrics listener from the MBeanServer", e);
      }
    }

    executorService.shutdown();

    meters.values().stream().flatMap(Collection::stream).forEach(meterRegistry::remove);
    meters.clear();
  }

  // visible for tests
  Set<Meter> getMeters() {
    return meters.values().stream().flatMap(Collection::stream).collect(toSet());
  }

  private List<Meter> computeMeters(ObjectName objectName, List<Meter> meters) {
    if (meters == null) {
      meters = new CopyOnWriteArrayList<>();
    }
    try {
      meters.addAll(metersFactory.createMeters(mBeanServer, objectName));
    } catch (JMException e) {
      throw new IllegalStateException(
          "Could not create meters for JMX object; most likely a programming error", e);
    }
    return meters;
  }

  private boolean isNotificationEnabled(Notification notification) {
    if (!handledNotificationTypes.contains(notification.getType())) {
      return false;
    }
    ObjectName objectName = ((MBeanServerNotification) notification).getMBeanName();
    return query.matches(objectName);
  }

  private void handleNotification(Notification notification, Object handback) {
    // we can't get JMX object attributes in the NotificationListener, so we'll do that
    // asynchronously on a separate thread
    executorService.submit(() -> handleNotificationAsync(notification));
  }

  private void handleNotificationAsync(Notification notification) {
    if (!(notification instanceof MBeanServerNotification)) {
      return;
    }
    ObjectName objectName = ((MBeanServerNotification) notification).getMBeanName();

    if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(notification.getType())) {
      meters.compute(objectName, this::computeMeters);
    } else if (MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(notification.getType())) {
      List<Meter> metersToRemove = meters.remove(objectName);
      for (Meter meter : metersToRemove) {
        meterRegistry.remove(meter);
      }
    }
  }
}
