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

package com.splunk.opentelemetry.hikaricp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.splunk.opentelemetry.testing.MeterData;
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
            new MeterData("db.pool.connections", "gauge", "connections", tags),
            new MeterData("db.pool.connections.active", "gauge", "connections", tags),
            new MeterData("db.pool.connections.idle", "gauge", "connections", tags),
            new MeterData("db.pool.connections.idle.min", "gauge", "connections", tags),
            new MeterData("db.pool.connections.max", "gauge", "connections", tags),
            new MeterData("db.pool.connections.pending_threads", "gauge", "threads", tags),
            new MeterData("db.pool.connections.timeouts", "counter", "timeouts", tags),
            new MeterData("db.pool.connections.create_time", "timer", "seconds", tags),
            new MeterData("db.pool.connections.wait_time", "timer", "seconds", tags),
            new MeterData("db.pool.connections.use_time", "timer", "seconds", tags));
  }
}
