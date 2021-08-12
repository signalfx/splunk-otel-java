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

package com.splunk.opentelemetry.instrumentation.c3p0;

import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_ACTIVE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_IDLE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_PENDING_THREADS;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_TOTAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.splunk.opentelemetry.testing.MeterId;
import com.splunk.opentelemetry.testing.TestMetricsAccess;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(AgentInstrumentationExtension.class)
public class C3p0InstrumentationTest {

  @BeforeAll
  static void setUpMocks() throws SQLException {
    MockDriver.register();
  }

  @AfterEach
  void clearMetrics() {
    TestMetricsAccess.clearMetrics();
  }

  @Test
  void shouldReportMetrics() throws Exception {
    // given
    var c3p0DataSource = new ComboPooledDataSource();
    c3p0DataSource.setDriverClass(MockDriver.class.getName());
    c3p0DataSource.setJdbcUrl("jdbc:mock:testDatabase");

    // when
    c3p0DataSource.getConnection().close();
    c3p0DataSource.getConnection().close();

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              var meters = TestMetricsAccess.getMeters();
              assertThat(meters).hasSize(4);
              assertConnectionPoolMetrics(meters, c3p0DataSource.getDataSourceName());
            });

    // when
    // this one too shouldn't cause any problems when called more than once
    c3p0DataSource.close();
    c3p0DataSource.close();

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(C3p0InstrumentationTest::assertNoConnectionPoolMetrics);
  }

  @Test
  void shouldHandleTwoDataSourcesWithSameIdentityToken() throws Exception {
    // given
    var dataSource1 = new ComboPooledDataSource();
    dataSource1.setDriverClass(MockDriver.class.getName());
    dataSource1.setJdbcUrl("jdbc:mock:testDatabase");
    dataSource1.setDataSourceName("dataSource1");
    dataSource1.setIdentityToken("test");

    var dataSource2 = new ComboPooledDataSource();
    dataSource2.setDriverClass(MockDriver.class.getName());
    dataSource2.setJdbcUrl("jdbc:mock:testDatabase");
    dataSource2.setDataSourceName("dataSource2");
    dataSource2.setIdentityToken("test");

    // when
    dataSource1.getConnection().close();
    dataSource2.getConnection().close();

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              var meters = TestMetricsAccess.getMeters();
              assertThat(meters).hasSize(8);
              assertConnectionPoolMetrics(meters, dataSource1.getDataSourceName());
              assertConnectionPoolMetrics(meters, dataSource2.getDataSourceName());
            });

    // when
    dataSource1.close();

    // then metrics from dataSource2 are still being recorded
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              var meters = TestMetricsAccess.getMeters();
              assertThat(meters).hasSize(4);
              assertConnectionPoolMetrics(meters, dataSource2.getDataSourceName());
            });

    // when
    dataSource2.close();

    // then there are no c3p0 metrics anymore
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(C3p0InstrumentationTest::assertNoConnectionPoolMetrics);
  }

  private static void assertConnectionPoolMetrics(Set<MeterId> meters, String poolName) {
    assertThat(poolName)
        .as("c3p0 generates a unique pool name if it's not explicitly provided")
        .isNotEmpty();

    var tags = Map.of("pool.name", poolName, "pool.type", "c3p0");

    assertThat(meters)
        .contains(
            MeterId.from(CONNECTIONS_TOTAL, tags),
            MeterId.from(CONNECTIONS_ACTIVE, tags),
            MeterId.from(CONNECTIONS_IDLE, tags),
            MeterId.from(CONNECTIONS_PENDING_THREADS, tags));
  }

  private static void assertNoConnectionPoolMetrics() {
    assertThat(TestMetricsAccess.getMeterNames())
        .doesNotContain(
            CONNECTIONS_TOTAL.name(),
            CONNECTIONS_ACTIVE.name(),
            CONNECTIONS_IDLE.name(),
            CONNECTIONS_PENDING_THREADS.name());
  }
}
