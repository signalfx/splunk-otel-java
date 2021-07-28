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

package com.splunk.opentelemetry.c3p0;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.splunk.opentelemetry.testing.MeterData;
import com.splunk.opentelemetry.testing.TestMetricsAccess;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(AgentInstrumentationExtension.class)
public class C3p0InstrumentationTest {
  @AfterEach
  void clearMetrics() {
    TestMetricsAccess.clearMetrics();
  }

  @Test
  void shouldReportMetrics() throws Exception {
    // given
    MockDriver.register();

    var c3p0DataSource = new ComboPooledDataSource();
    c3p0DataSource.setDriverClass(MockDriver.class.getName());
    c3p0DataSource.setJdbcUrl("jdbc:mock:testDatabase");

    // when
    c3p0DataSource.getConnection().close();
    c3p0DataSource.getConnection().close();

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(() -> assertConnectionPoolMetrics(c3p0DataSource.getDataSourceName()));

    // when
    // this one too shouldn't cause any problems when called more than once
    c3p0DataSource.close();
    c3p0DataSource.close();

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(C3p0InstrumentationTest::assertNoConnectionPoolMetrics);
  }

  private static void assertConnectionPoolMetrics(String poolName) {
    assertThat(poolName)
        .as("c3p0 generates a unique pool name if it's not explicitly provided")
        .isNotEmpty();

    var tags = Map.of("pool.name", poolName, "pool.type", "c3p0");

    assertThat(TestMetricsAccess.getMeters())
        .containsExactlyInAnyOrder(
            new MeterData("db.pool.connections", "gauge", "connections", tags),
            new MeterData("db.pool.connections.active", "gauge", "connections", tags),
            new MeterData("db.pool.connections.idle", "gauge", "connections", tags),
            new MeterData("db.pool.connections.pending_threads", "gauge", "threads", tags));
  }

  private static void assertNoConnectionPoolMetrics() {
    assertThat(TestMetricsAccess.getMeters().stream().map(MeterData::getName).distinct())
        .doesNotContain(
            "db.pool.connections",
            "db.pool.connections.active",
            "db.pool.connections.idle",
            "db.pool.connections.pending_threads");
  }
}
