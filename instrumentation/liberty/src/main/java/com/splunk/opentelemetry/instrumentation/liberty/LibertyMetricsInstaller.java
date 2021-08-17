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

package com.splunk.opentelemetry.instrumentation.liberty;

import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ThreadPoolSemanticConventions.EXECUTOR_NAME;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ThreadPoolSemanticConventions.EXECUTOR_TYPE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ThreadPoolSemanticConventions.THREADS_ACTIVE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.ThreadPoolSemanticConventions.THREADS_CURRENT;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.jmx.JmxAttributesHelper.getNumberAttribute;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

import com.google.auto.service.AutoService;
import com.splunk.opentelemetry.javaagent.bootstrap.metrics.jmx.JmxMetricsWatcher;
import com.splunk.opentelemetry.javaagent.bootstrap.metrics.jmx.JmxQuery;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.extension.AgentListener;
import java.util.List;
import javax.management.MBeanServer;
import javax.management.ObjectName;

@AutoService(AgentListener.class)
public class LibertyMetricsInstaller implements AgentListener {

  private final JmxMetricsWatcher jmxMetricsWatcher =
      new JmxMetricsWatcher(
          JmxQuery.create("WebSphere", "type", "ThreadPoolStats"), this::createMeters);

  @Override
  public void afterAgent(Config config) {
    boolean metricsRegistryPresent = !Metrics.globalRegistry.getRegistries().isEmpty();
    if (!config.isInstrumentationEnabled(singleton("liberty"), metricsRegistryPresent)) {
      return;
    }
    jmxMetricsWatcher.start();
  }

  private List<Meter> createMeters(MBeanServer mBeanServer, ObjectName objectName) {
    Tags tags = executorTags(objectName);
    return asList(
        THREADS_CURRENT.create(tags, mBeanServer, getNumberAttribute(objectName, "PoolSize")),
        THREADS_ACTIVE.create(tags, mBeanServer, getNumberAttribute(objectName, "ActiveThreads")));
  }

  private static Tags executorTags(ObjectName objectName) {
    // use the "name" property if available
    String name = objectName.getKeyProperty("name");
    // if its unavailable just use the whole mbean name
    if (name == null) {
      name = objectName.toString();
    }
    return Tags.of(Tag.of(EXECUTOR_TYPE, "liberty"), Tag.of(EXECUTOR_NAME, name));
  }
}
