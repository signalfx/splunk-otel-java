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

package com.splunk.opentelemetry.instrumentation.tomcatjdbc;

import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ConnectionPoolSemanticConventions.CONNECTIONS_ACTIVE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ConnectionPoolSemanticConventions.CONNECTIONS_IDLE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ConnectionPoolSemanticConventions.CONNECTIONS_IDLE_MAX;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ConnectionPoolSemanticConventions.CONNECTIONS_IDLE_MIN;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ConnectionPoolSemanticConventions.CONNECTIONS_MAX;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ConnectionPoolSemanticConventions.CONNECTIONS_PENDING_THREADS;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.ConnectionPoolSemanticConventions.CONNECTIONS_TOTAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;

import com.splunk.opentelemetry.testing.MeterId;
import com.splunk.opentelemetry.testing.TestMetricsAccess;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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

    // there shouldn't be any problems if this methods gets called more than once
    tomcatDataSource.createPool();
    tomcatDataSource.createPool();

    // when
    var connection = tomcatDataSource.getConnection();
    connection.close();

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(() -> assertConnectionPoolMetrics(tomcatDataSource.getPoolName()));

    // when
    // this one too shouldn't cause any problems when called more than once
    tomcatDataSource.close();
    tomcatDataSource.close();

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(TomcatJdbcInstrumentationTest::assertNoConnectionPoolMetrics);
  }

  private static void assertConnectionPoolMetrics(String poolName) {
    assertThat(poolName)
        .as("tomcat-jdbc generates a unique pool name if it's not explicitly provided")
        .isNotEmpty();

    var tags =
        Map.of(
            "pool.name", poolName, "pool.type", "tomcat-jdbc", "service", "unknown_service:java");

    assertThat(TestMetricsAccess.getMeters())
        .containsExactlyInAnyOrder(
            MeterId.from(CONNECTIONS_TOTAL, tags),
            MeterId.from(CONNECTIONS_ACTIVE, tags),
            MeterId.from(CONNECTIONS_IDLE, tags),
            MeterId.from(CONNECTIONS_IDLE_MIN, tags),
            MeterId.from(CONNECTIONS_IDLE_MAX, tags),
            MeterId.from(CONNECTIONS_MAX, tags),
            MeterId.from(CONNECTIONS_PENDING_THREADS, tags));
  }

  private static void assertNoConnectionPoolMetrics() {
    assertThat(TestMetricsAccess.getMeterNames())
        .doesNotContain(
            CONNECTIONS_TOTAL.name(),
            CONNECTIONS_ACTIVE.name(),
            CONNECTIONS_IDLE.name(),
            CONNECTIONS_IDLE_MIN.name(),
            CONNECTIONS_IDLE_MAX.name(),
            CONNECTIONS_MAX.name(),
            CONNECTIONS_PENDING_THREADS.name());
  }
}
