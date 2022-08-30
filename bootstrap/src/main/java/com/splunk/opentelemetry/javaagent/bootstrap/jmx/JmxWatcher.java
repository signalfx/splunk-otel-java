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

package com.splunk.opentelemetry.javaagent.bootstrap.jmx;

import static java.util.Arrays.asList;
import static java.util.logging.Level.WARNING;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Logger;
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

public final class JmxWatcher {

  private static final Logger logger = Logger.getLogger(JmxWatcher.class.getName());

  private static final AtomicInteger threadIds = new AtomicInteger();
  private static final ThreadFactory daemonThreadFactory =
      r -> {
        Thread t = new Thread(r, "JmxWatcher-" + threadIds.incrementAndGet());
        t.setDaemon(true);
        t.setContextClassLoader(null);
        return t;
      };

  private static final Set<String> handledNotificationTypes =
      new HashSet<>(
          asList(
              MBeanServerNotification.REGISTRATION_NOTIFICATION,
              MBeanServerNotification.UNREGISTRATION_NOTIFICATION));

  // lazy-initialize the MBeanServer to avoid loading JMX classes too early - that could cause JUL
  // classloading hell in e.g. JBoss deployments
  private final Supplier<MBeanServer> mBeanServerSupplier;
  private final ExecutorService executorService;
  private final JmxQuery query;
  private final JmxListener listener;

  private volatile MBeanServer mBeanServer;
  private volatile NotificationListener notificationListener;

  public JmxWatcher(JmxQuery query, JmxListener listener) {
    this(
        JmxWatcher::findMBeanServer,
        Executors.newSingleThreadExecutor(daemonThreadFactory),
        query,
        listener);
  }

  // visible for tests
  JmxWatcher(
      Supplier<MBeanServer> mBeanServerSupplier,
      ExecutorService executorService,
      JmxQuery query,
      JmxListener listener) {
    this.mBeanServerSupplier = mBeanServerSupplier;
    this.executorService = executorService;
    this.query = query;
    this.listener = listener;
  }

  // copied from micrometer CommonsObjectPool2Metrics class
  private static MBeanServer findMBeanServer() {
    List<MBeanServer> mBeanServers = MBeanServerFactory.findMBeanServer(null);
    return mBeanServers.isEmpty()
        ? ManagementFactory.getPlatformMBeanServer()
        : mBeanServers.get(0);
  }

  public void start() {
    try {
      mBeanServer = mBeanServerSupplier.get();

      // get all mbeans matching the query, call the listener for them
      Set<ObjectName> existingObjects = mBeanServer.queryNames(query.toObjectNameQuery(), null);
      for (ObjectName existingObject : existingObjects) {
        listener.onRegister(mBeanServer, existingObject);
      }

      // register a notification listener that'll call the JmxListener whenever objects matching
      // JmxQuery get registered/unregistered
      NotificationFilter filter = this::isNotificationEnabled;
      notificationListener = this::handleNotificationAsynchronously;
      mBeanServer.addNotificationListener(
          MBeanServerDelegate.DELEGATE_NAME, notificationListener, filter, null);
    } catch (JMException e) {
      logger.log(WARNING, "Could not start the JmxWatcher", e);
    }
  }

  public void stop() {
    if (notificationListener != null) {
      try {
        mBeanServer.removeNotificationListener(
            MBeanServerDelegate.DELEGATE_NAME, notificationListener);
        notificationListener = null;
      } catch (InstanceNotFoundException | ListenerNotFoundException e) {
        logger.log(WARNING, "Could not remove the NotificationListener from the MBeanServer", e);
      }
    }

    executorService.shutdown();
  }

  private boolean isNotificationEnabled(Notification notification) {
    if (!handledNotificationTypes.contains(notification.getType())) {
      return false;
    }
    ObjectName objectName = ((MBeanServerNotification) notification).getMBeanName();
    return query.matches(objectName);
  }

  private void handleNotificationAsynchronously(Notification notification, Object handback) {
    // we can't get JMX object attributes in the NotificationListener, so we'll do that
    // asynchronously on a separate thread
    executorService.submit(() -> handleNotification(notification));
  }

  private void handleNotification(Notification notification) {
    if (!(notification instanceof MBeanServerNotification)) {
      return;
    }
    ObjectName objectName = ((MBeanServerNotification) notification).getMBeanName();

    try {
      if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(notification.getType())) {
        listener.onRegister(mBeanServer, objectName);
      } else if (MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(
          notification.getType())) {
        listener.onUnregister(mBeanServer, objectName);
      }
    } catch (JMException e) {
      logger.log(WARNING, "JMX exception thrown in JmxListener", e);
    }
  }
}
