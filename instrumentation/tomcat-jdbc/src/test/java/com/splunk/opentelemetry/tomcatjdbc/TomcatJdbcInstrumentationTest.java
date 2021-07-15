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

package com.splunk.opentelemetry.tomcatjdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;

import com.splunk.opentelemetry.testing.MeterData;
import com.splunk.opentelemetry.testing.TestMetricsAccess;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.management.ObjectName;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(AgentInstrumentationExtension.class)
public class TomcatJdbcInstrumentationTest {
  @Mock javax.sql.DataSource dataSourceMock;
  @Mock Connection connectionMock;

  @AfterEach
  void clearMetrics() {
    TestMetricsAccess.clearMetrics();
  }

  @Test
  void shouldReportMetrics() throws Exception {
    // given
    given(dataSourceMock.getConnection()).willReturn(connectionMock);

    var tomcatDataSource = new DataSource();
    tomcatDataSource.setDataSource(dataSourceMock);

    // this is the recommended way to setup the connection pool outside of tomcat:
    // https://tomcat.apache.org/tomcat-8.5-doc/jdbc-pool.html#JMX
    tomcatDataSource.createPool();
    var objectName =
        new ObjectName("org.apache.tomcat.jdbc.pool.jmx.ConnectionPool:name=testConnectionPool");
    ManagementFactory.getPlatformMBeanServer()
        .registerMBean(tomcatDataSource.getPool().getJmxPool(), objectName);

    // when
    var connection = tomcatDataSource.getConnection();
    connection.close();

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(() -> assertConnectionPoolMetrics(tomcatDataSource.getPoolName()));

    // when
    ManagementFactory.getPlatformMBeanServer().unregisterMBean(objectName);

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(TomcatJdbcInstrumentationTest::assertNoConnectionPoolMetrics);
  }

  private static void assertConnectionPoolMetrics(String poolName) {
    assertThat(poolName)
        .as("tomcat-jdbc generates a unique pool name if it's not explicitly provided")
        .isNotEmpty();

    var tags = Map.of("pool.name", poolName, "pool.type", "tomcat-jdbc");

    assertThat(TestMetricsAccess.getMeters())
        .containsExactlyInAnyOrder(
            new MeterData("db.pool.connections", "gauge", "connections", tags),
            new MeterData("db.pool.connections.active", "gauge", "connections", tags),
            new MeterData("db.pool.connections.idle", "gauge", "connections", tags),
            new MeterData("db.pool.connections.idle.min", "gauge", "connections", tags),
            new MeterData("db.pool.connections.idle.max", "gauge", "connections", tags),
            new MeterData("db.pool.connections.max", "gauge", "connections", tags),
            new MeterData("db.pool.connections.pending_threads", "gauge", "threads", tags));
  }

  private static void assertNoConnectionPoolMetrics() {
    assertThat(TestMetricsAccess.getMeters().stream().map(MeterData::getName).distinct())
        .doesNotContain(
            "db.pool.connections",
            "db.pool.connections.active",
            "db.pool.connections.idle",
            "db.pool.connections.idle.min",
            "db.pool.connections.idle.max",
            "db.pool.connections.max",
            "db.pool.connections.pending_threads");
  }
}
