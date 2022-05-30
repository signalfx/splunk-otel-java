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

package com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.jmx;

import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.jmx.JmxAttributesHelper.getNumberAttribute;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.splunk.opentelemetry.javaagent.bootstrap.jmx.JmxQuery;
import com.splunk.opentelemetry.javaagent.bootstrap.metrics.jmx.TestClass;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.search.Search;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.jupiter.api.Test;

class JmxMetricsWatcherIntegrationTest {
  @Test
  void test() throws Exception {
    var objectName1 = new ObjectName("com.splunk.test:type=Test,name=micrometer-watcher-test-1");
    ManagementFactory.getPlatformMBeanServer().registerMBean(new TestClass(12), objectName1);

    var watcher =
        new JmxMetricsWatcher(
            JmxQuery.create("com.splunk.test", Map.of("type", "Test")),
            JmxMetricsWatcherIntegrationTest::createMeters);
    watcher.start();

    await().atMost(5, SECONDS).untilAsserted(() -> assertEquals(1, getTestMeters().size()));

    var objectName2 = new ObjectName("com.splunk.test:type=Test,name=micrometer-watcher-test-2");
    ManagementFactory.getPlatformMBeanServer().registerMBean(new TestClass(42), objectName2);

    await().atMost(5, SECONDS).untilAsserted(() -> assertEquals(2, getTestMeters().size()));

    ManagementFactory.getPlatformMBeanServer().unregisterMBean(objectName2);

    await().atMost(5, SECONDS).untilAsserted(() -> assertEquals(1, getTestMeters().size()));

    watcher.stop();

    await().atMost(5, SECONDS).untilAsserted(() -> assertTrue(getTestMeters().isEmpty()));

    ManagementFactory.getPlatformMBeanServer().unregisterMBean(objectName1);
  }

  private static List<Meter> createMeters(MBeanServer mBeanServer, ObjectName objectName) {
    Gauge gauge =
        Gauge.builder("test_gauge", mBeanServer, getNumberAttribute(objectName, "foo"))
            .tags("name", objectName.getKeyProperty("name"))
            .register(Metrics.globalRegistry);
    return List.of(gauge);
  }

  private Collection<Meter> getTestMeters() {
    return Search.in(Metrics.globalRegistry).name("test_gauge").meters();
  }
}
