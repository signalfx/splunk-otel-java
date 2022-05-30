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

import com.splunk.opentelemetry.javaagent.bootstrap.jmx.JmxListener;
import com.splunk.opentelemetry.javaagent.bootstrap.jmx.JmxQuery;
import com.splunk.opentelemetry.javaagent.bootstrap.jmx.JmxWatcher;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public abstract class AbstractJmxMetricsWatcher<T> implements JmxListener {

  private final JmxWatcher jmxWatcher;
  private final Map<ObjectName, List<T>> meters = new ConcurrentHashMap<>();
  private final MetersFactory<T> metersFactory;

  protected AbstractJmxMetricsWatcher(JmxQuery query, MetersFactory<T> metersFactory) {
    this.jmxWatcher = new JmxWatcher(query, this);
    this.metersFactory = metersFactory;
  }

  protected AbstractJmxMetricsWatcher(JmxWatcher jmxWatcher, MetersFactory<T> metersFactory) {
    this.jmxWatcher = jmxWatcher;
    this.metersFactory = metersFactory;
  }

  public void start() {
    jmxWatcher.start();
  }

  public void stop() {
    jmxWatcher.stop();
    getAllMeters().forEach(this::unregister);
    meters.clear();
  }

  // visible for tests
  public Stream<T> getAllMeters() {
    return meters.values().stream().flatMap(Collection::stream);
  }

  @Override
  public void onRegister(MBeanServer mBeanServer, ObjectName objectName) {
    meters.compute(objectName, (name, meters) -> computeMeters(mBeanServer, name, meters));
  }

  private List<T> computeMeters(MBeanServer mBeanServer, ObjectName objectName, List<T> meters) {
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

  @Override
  public void onUnregister(MBeanServer mBeanServer, ObjectName objectName) {
    List<T> metersToRemove = meters.remove(objectName);
    for (T meter : metersToRemove) {
      unregister(meter);
    }
  }

  protected abstract void unregister(T meter);
}
