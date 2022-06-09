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

package com.splunk.opentelemetry.instrumentation.commonsdbcp2;

import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ConnectionPoolSemanticConventions.CONNECTIONS_ACTIVE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ConnectionPoolSemanticConventions.CONNECTIONS_IDLE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ConnectionPoolSemanticConventions.CONNECTIONS_IDLE_MAX;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ConnectionPoolSemanticConventions.CONNECTIONS_IDLE_MIN;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ConnectionPoolSemanticConventions.CONNECTIONS_MAX;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ConnectionPoolSemanticConventions.CONNECTIONS_TOTAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

import com.splunk.opentelemetry.testing.MeterId;
import com.splunk.opentelemetry.testing.TestMetricsAccess;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.Driver;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.management.ObjectName;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(AgentInstrumentationExtension.class)
public class CommonsDbcp2InstrumentationTest {
  @Mock Driver driverMock;
  @Mock Connection connectionMock;

  @AfterEach
  void clearMetrics() {
    TestMetricsAccess.clearMetrics();
  }

  @Test
  void shouldReportMetricsForConnectionPool() throws Exception {
    // given
    given(driverMock.connect(any(), any())).willReturn(connectionMock);
    given(connectionMock.isValid(anyInt())).willReturn(true);

    var connectionPool = new BasicDataSource();
    connectionPool.setDriver(driverMock);
    connectionPool.setUrl("db:///url");
    connectionPool.setJmxName("com.splunk.db:pool=connectionPool");

    // when
    connectionPool.getConnection();

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(() -> assertConnectionPoolMetrics(connectionPool.getJmxName()));
  }

  // Spring does not rely on BasicDataSource's built-in JMX registration - it registers each MXBean
  // that it finds in the context instead
  @Test
  void shouldReportMetricsForConnectionPool_springBootLikeScenario() throws Exception {
    // given
    var connectionPool = new BasicDataSource();
    connectionPool.setDriver(driverMock);
    connectionPool.setUrl("db:///url");

    var objectName = new ObjectName("org.apache.commons.dbcp2.BasicDataSource:name=dataSource");

    // when
    ManagementFactory.getPlatformMBeanServer().registerMBean(connectionPool, objectName);

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(() -> assertConnectionPoolMetrics("dataSource"));

    // when
    ManagementFactory.getPlatformMBeanServer().unregisterMBean(objectName);

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(CommonsDbcp2InstrumentationTest::assertNoConnectionPoolMetrics);
  }

  private static void assertConnectionPoolMetrics(String poolName) {
    var tags = Map.of("pool.name", poolName, "pool.type", "dbcp2");

    assertThat(TestMetricsAccess.getMeters())
        .containsExactlyInAnyOrder(
            MeterId.from(CONNECTIONS_TOTAL, tags),
            MeterId.from(CONNECTIONS_ACTIVE, tags),
            MeterId.from(CONNECTIONS_IDLE, tags),
            MeterId.from(CONNECTIONS_IDLE_MIN, tags),
            MeterId.from(CONNECTIONS_IDLE_MAX, tags),
            MeterId.from(CONNECTIONS_MAX, tags));
  }

  private static void assertNoConnectionPoolMetrics() {
    assertThat(TestMetricsAccess.getMeterNames())
        .doesNotContain(
            CONNECTIONS_TOTAL.name(),
            CONNECTIONS_ACTIVE.name(),
            CONNECTIONS_IDLE.name(),
            CONNECTIONS_IDLE_MIN.name(),
            CONNECTIONS_IDLE_MAX.name(),
            CONNECTIONS_MAX.name());
  }
}
