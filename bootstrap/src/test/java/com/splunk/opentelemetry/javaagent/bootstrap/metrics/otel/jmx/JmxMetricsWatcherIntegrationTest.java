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

package com.splunk.opentelemetry.javaagent.bootstrap.metrics.otel.jmx;

import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.jmx.JmxAttributesHelper.getNumberAttribute;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.splunk.opentelemetry.javaagent.bootstrap.jmx.JmxQuery;
import com.splunk.opentelemetry.javaagent.bootstrap.metrics.jmx.TestClass;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.jupiter.api.Test;

class JmxMetricsWatcherIntegrationTest {
  private static List<AutoCloseable> testMeters = Collections.synchronizedList(new ArrayList<>());

  @Test
  void test() throws Exception {
    var objectName1 = new ObjectName("com.splunk.test:type=Test,name=otel-watcher-test-1");
    ManagementFactory.getPlatformMBeanServer().registerMBean(new TestClass(12), objectName1);

    var watcher =
        new JmxMetricsWatcher(
            JmxQuery.create("com.splunk.test", Map.of("type", "Test")),
            JmxMetricsWatcherIntegrationTest::createMeters);
    watcher.start();

    await().atMost(5, SECONDS).untilAsserted(() -> assertEquals(1, getTestMeters().size()));

    var objectName2 = new ObjectName("com.splunk.test:type=Test,name=otel-watcher-test-2");
    ManagementFactory.getPlatformMBeanServer().registerMBean(new TestClass(42), objectName2);

    await().atMost(5, SECONDS).untilAsserted(() -> assertEquals(2, getTestMeters().size()));

    ManagementFactory.getPlatformMBeanServer().unregisterMBean(objectName2);

    await().atMost(5, SECONDS).untilAsserted(() -> assertEquals(1, getTestMeters().size()));

    watcher.stop();

    await().atMost(5, SECONDS).untilAsserted(() -> assertTrue(getTestMeters().isEmpty()));

    ManagementFactory.getPlatformMBeanServer().unregisterMBean(objectName1);
  }

  private static List<AutoCloseable> createMeters(MBeanServer mBeanServer, ObjectName objectName) {
    OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
    Attributes attributes = Attributes.of(stringKey("name"), objectName.getKeyProperty("name"));
    ObservableDoubleGauge gauge =
        openTelemetry
            .getMeter("jmx-test")
            .gaugeBuilder("test_gauge")
            .buildWithCallback(
                measurement ->
                    measurement.record(
                        getNumberAttribute(objectName, "foo").applyAsDouble(mBeanServer),
                        attributes));

    AutoCloseable result =
        new AutoCloseable() {
          @Override
          public void close() {
            gauge.close();
            testMeters.remove(this);
          }
        };
    testMeters.add(result);
    return List.of(result);
  }

  private Collection<AutoCloseable> getTestMeters() {
    return testMeters;
  }
}
