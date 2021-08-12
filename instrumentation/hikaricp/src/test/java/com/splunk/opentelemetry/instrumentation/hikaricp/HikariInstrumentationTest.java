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

package com.splunk.opentelemetry.instrumentation.hikaricp;

import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_ACTIVE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_CREATE_TIME;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_IDLE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_IDLE_MIN;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_MAX;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_PENDING_THREADS;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_TIMEOUTS;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_TOTAL;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_USE_TIME;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_WAIT_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.splunk.opentelemetry.testing.MeterId;
import com.splunk.opentelemetry.testing.TestMetricsAccess;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.IMetricsTracker;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(AgentInstrumentationExtension.class)
public class HikariInstrumentationTest {
  @Mock DataSource dataSourceMock;
  @Mock Connection connectionMock;
  @Mock IMetricsTracker userMetricsMock;

  @AfterEach
  void clearMetrics() {
    TestMetricsAccess.clearMetrics();
  }

  @Test
  void shouldReportMetrics() throws Exception {
    // given
    given(dataSourceMock.getConnection()).willReturn(connectionMock);

    var hikariConfig = new HikariConfig();
    hikariConfig.setPoolName("testPool");
    hikariConfig.setDataSource(dataSourceMock);

    var hikariDataSource = new HikariDataSource(hikariConfig);

    // when
    var hikariConnection = hikariDataSource.getConnection();
    TimeUnit.MILLISECONDS.sleep(10);
    hikariConnection.close();

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(HikariInstrumentationTest::assertConnectionPoolMetrics);

    // when
    hikariDataSource.close();

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(HikariInstrumentationTest::assertNoConnectionPoolMetrics);
  }

  @Test
  void shouldNotBreakCustomUserMetrics() throws Exception {
    // given
    given(dataSourceMock.getConnection()).willReturn(connectionMock);

    var hikariConfig = new HikariConfig();
    hikariConfig.setPoolName("testPool");
    hikariConfig.setDataSource(dataSourceMock);

    var hikariDataSource = new HikariDataSource(hikariConfig);
    hikariDataSource.setMetricsTrackerFactory(((poolName, poolStats) -> userMetricsMock));

    // when
    var hikariConnection = hikariDataSource.getConnection();
    TimeUnit.MILLISECONDS.sleep(10);
    hikariConnection.close();

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(HikariInstrumentationTest::assertConnectionPoolMetrics);

    verify(userMetricsMock, atLeastOnce()).recordConnectionCreatedMillis(anyLong());
    verify(userMetricsMock, atLeastOnce()).recordConnectionAcquiredNanos(anyLong());
    verify(userMetricsMock, atLeastOnce()).recordConnectionUsageMillis(anyLong());
  }

  private static void assertConnectionPoolMetrics() {
    var tags = Map.of("pool.name", "testPool", "pool.type", "hikari");

    assertThat(TestMetricsAccess.getMeters())
        .containsExactlyInAnyOrder(
            MeterId.from(CONNECTIONS_TOTAL, tags),
            MeterId.from(CONNECTIONS_ACTIVE, tags),
            MeterId.from(CONNECTIONS_IDLE, tags),
            MeterId.from(CONNECTIONS_IDLE_MIN, tags),
            MeterId.from(CONNECTIONS_MAX, tags),
            MeterId.from(CONNECTIONS_PENDING_THREADS, tags),
            MeterId.from(CONNECTIONS_TIMEOUTS, tags),
            MeterId.from(CONNECTIONS_CREATE_TIME, tags),
            MeterId.from(CONNECTIONS_WAIT_TIME, tags),
            MeterId.from(CONNECTIONS_USE_TIME, tags));
  }

  private static void assertNoConnectionPoolMetrics() {
    assertThat(TestMetricsAccess.getMeterNames())
        .doesNotContain(
            CONNECTIONS_TOTAL.name(),
            CONNECTIONS_ACTIVE.name(),
            CONNECTIONS_IDLE.name(),
            CONNECTIONS_IDLE.name(),
            CONNECTIONS_MAX.name(),
            CONNECTIONS_PENDING_THREADS.name(),
            CONNECTIONS_TIMEOUTS.name(),
            CONNECTIONS_CREATE_TIME.name(),
            CONNECTIONS_WAIT_TIME.name(),
            CONNECTIONS_USE_TIME.name());
  }
}
