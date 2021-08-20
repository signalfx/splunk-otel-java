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

import static javax.management.MBeanServerNotification.REGISTRATION_NOTIFICATION;
import static javax.management.MBeanServerNotification.UNREGISTRATION_NOTIFICATION;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JmxWatcherTest {

  @Mock MBeanServer mBeanServer;
  @Mock ExecutorService executorService;
  @Mock JmxListener listener;

  JmxQuery query = JmxQuery.create("com.splunk.test", "type", "TestType");

  JmxWatcher underTest;

  @Captor ArgumentCaptor<NotificationFilter> notificationFilterCaptor;
  @Captor ArgumentCaptor<NotificationListener> notificationListenerCaptor;
  @Captor ArgumentCaptor<Runnable> runnableCaptor;

  @BeforeEach
  void setUp() {
    underTest = new JmxWatcher(() -> mBeanServer, executorService, query, listener);
  }

  @Test
  void shouldRegisterMetersForAlreadyExistingJmxObjects() throws Exception {
    // given
    var objectName1 = new ObjectName("com.splunk.test:type=TestType,name=test1");
    var objectName2 = new ObjectName("com.splunk.test:type=TestType,name=test2");

    when(mBeanServer.queryNames(query.toObjectNameQuery(), null))
        .thenReturn(Set.of(objectName1, objectName2));

    // when
    underTest.start();

    // then
    verify(listener).onRegister(mBeanServer, objectName1);
    verify(listener).onRegister(mBeanServer, objectName2);
  }

  @Test
  void shouldFilterNotifications() throws Exception {
    // given
    when(mBeanServer.queryNames(query.toObjectNameQuery(), null)).thenReturn(Set.of());

    underTest.start();
    verify(mBeanServer)
        .addNotificationListener(
            eq(MBeanServerDelegate.DELEGATE_NAME),
            any(NotificationListener.class),
            notificationFilterCaptor.capture(),
            isNull());

    NotificationFilter filter = notificationFilterCaptor.getValue();

    var wrongObjectName = new ObjectName("com.splunk.wrong_object_name:type=WrongType");
    var correctObjectName = new ObjectName("com.splunk.test:type=TestType");

    // when-then
    assertFalse(filter.isNotificationEnabled(notification("some custom type", correctObjectName)));
    assertFalse(
        filter.isNotificationEnabled(notification(REGISTRATION_NOTIFICATION, wrongObjectName)));
    assertFalse(
        filter.isNotificationEnabled(notification(UNREGISTRATION_NOTIFICATION, wrongObjectName)));
    assertTrue(
        filter.isNotificationEnabled(notification(REGISTRATION_NOTIFICATION, correctObjectName)));
    assertTrue(
        filter.isNotificationEnabled(notification(UNREGISTRATION_NOTIFICATION, correctObjectName)));
  }

  @Test
  void shouldHandleRegisterNotification() throws Exception {
    // given
    when(mBeanServer.queryNames(query.toObjectNameQuery(), null)).thenReturn(Set.of());

    underTest.start();
    verify(mBeanServer)
        .addNotificationListener(
            eq(MBeanServerDelegate.DELEGATE_NAME),
            notificationListenerCaptor.capture(),
            any(),
            isNull());

    NotificationListener notificationListener = notificationListenerCaptor.getValue();

    // when
    var objectName = new ObjectName("com.splunk.test:type=TestType,name=test");
    notificationListener.handleNotification(
        notification(REGISTRATION_NOTIFICATION, objectName), null);
    // capture and immediately run async listener method
    verify(executorService).submit(runnableCaptor.capture());
    runnableCaptor.getValue().run();

    // then
    verify(listener).onRegister(mBeanServer, objectName);
  }

  @Test
  void shouldHandleUnregisterNotification() throws Exception {
    // given
    when(mBeanServer.queryNames(query.toObjectNameQuery(), null)).thenReturn(Set.of());

    underTest.start();
    verify(mBeanServer)
        .addNotificationListener(
            eq(MBeanServerDelegate.DELEGATE_NAME),
            notificationListenerCaptor.capture(),
            any(),
            isNull());

    NotificationListener notificationListener = notificationListenerCaptor.getValue();

    // when
    var objectName = new ObjectName("com.splunk.test:type=TestType,name=test");
    notificationListener.handleNotification(
        notification(UNREGISTRATION_NOTIFICATION, objectName), null);
    // capture and immediately run async listener method
    verify(executorService).submit(runnableCaptor.capture());
    runnableCaptor.getValue().run();

    // then
    verify(listener).onUnregister(mBeanServer, objectName);
  }

  @Test
  void shouldStop() throws Exception {
    // given
    when(mBeanServer.queryNames(query.toObjectNameQuery(), null)).thenReturn(Set.of());

    underTest.start();
    verify(mBeanServer)
        .addNotificationListener(
            eq(MBeanServerDelegate.DELEGATE_NAME),
            notificationListenerCaptor.capture(),
            any(),
            isNull());

    // when
    underTest.stop();

    // then
    verify(mBeanServer)
        .removeNotificationListener(
            MBeanServerDelegate.DELEGATE_NAME, notificationListenerCaptor.getValue());
    verify(executorService).shutdown();
  }

  private static Notification notification(String type, ObjectName objectName) {
    return new MBeanServerNotification(type, new Object(), 0, objectName);
  }
}
