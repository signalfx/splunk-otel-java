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

package com.splunk.opentelemetry.instrumentation.oracleucp;

import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_ACTIVE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_IDLE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_MAX;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_PENDING_THREADS;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_TOTAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.splunk.opentelemetry.testing.MeterId;
import com.splunk.opentelemetry.testing.TestMetricsAccess;
import com.splunk.opentelemetry.testing.db.MockDriver;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(AgentInstrumentationExtension.class)
public class UcpInstrumentationTest {

  @BeforeAll
  static void setUpMocks() throws SQLException {
    MockDriver.register();
  }

  @AfterEach
  void clearMetrics() {
    TestMetricsAccess.clearMetrics();
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldReportMetrics(boolean setExplicitPoolName) throws Exception {
    // given
    var connectionPool = PoolDataSourceFactory.getPoolDataSource();
    connectionPool.setConnectionFactoryClassName(MockDriver.class.getName());
    connectionPool.setURL("jdbc:mock:testDatabase");
    if (setExplicitPoolName) {
      connectionPool.setConnectionPoolName("testPool");
    }

    // when
    connectionPool.getConnection().close();
    connectionPool.getConnection().close();

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(() -> assertConnectionPoolMetrics(connectionPool.getConnectionPoolName()));

    // when
    UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager()
        .destroyConnectionPool(connectionPool.getConnectionPoolName());

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(UcpInstrumentationTest::assertNoConnectionPoolMetrics);
  }

  private static void assertConnectionPoolMetrics(String poolName) {
    assertThat(poolName)
        .as("UCP generates a unique pool name if it's not explicitly provided")
        .isNotEmpty();

    var tags = Map.of("pool.name", poolName, "pool.type", "oracle-ucp");

    assertThat(TestMetricsAccess.getMeters())
        .containsExactlyInAnyOrder(
            MeterId.from(CONNECTIONS_TOTAL, tags),
            MeterId.from(CONNECTIONS_ACTIVE, tags),
            MeterId.from(CONNECTIONS_IDLE, tags),
            MeterId.from(CONNECTIONS_MAX, tags),
            MeterId.from(CONNECTIONS_PENDING_THREADS, tags));
  }

  private static void assertNoConnectionPoolMetrics() {
    assertThat(TestMetricsAccess.getMeterNames())
        .doesNotContain(
            CONNECTIONS_TOTAL.name(),
            CONNECTIONS_ACTIVE.name(),
            CONNECTIONS_IDLE.name(),
            CONNECTIONS_MAX.name(),
            CONNECTIONS_PENDING_THREADS.name());
  }
}
