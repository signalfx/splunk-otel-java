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

package com.splunk.opentelemetry.viburdbcp;

import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_ACTIVE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_IDLE;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_MAX;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.DataSourceSemanticConventions.CONNECTIONS_TOTAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;

import com.splunk.opentelemetry.testing.MeterId;
import com.splunk.opentelemetry.testing.TestMetricsAccess;
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
import org.vibur.dbcp.ViburDBCPDataSource;

@ExtendWith(MockitoExtension.class)
@ExtendWith(AgentInstrumentationExtension.class)
public class ViburDbcpInstrumentationTest {
  @Mock DataSource dataSourceMock;
  @Mock Connection connectionMock;

  @AfterEach
  void clearMetrics() {
    TestMetricsAccess.clearMetrics();
  }

  @Test
  void shouldReportMetrics() throws Exception {
    // given
    given(dataSourceMock.getConnection()).willReturn(connectionMock);

    var viburDataSource = new ViburDBCPDataSource();
    viburDataSource.setExternalDataSource(dataSourceMock);
    viburDataSource.start();

    // when
    var connection = viburDataSource.getConnection();
    connection.close();

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(() -> assertConnectionPoolMetrics(viburDataSource.getName()));

    // when
    // this one too shouldn't cause any problems when called more than once
    viburDataSource.close();
    viburDataSource.close();

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(ViburDbcpInstrumentationTest::assertNoConnectionPoolMetrics);
  }

  private static void assertConnectionPoolMetrics(String poolName) {
    assertThat(poolName)
        .as("vibur-dbcp generates a unique pool name if it's not explicitly provided")
        .isNotEmpty();

    var tags = Map.of("pool.name", poolName, "pool.type", "vibur-dbcp");

    assertThat(TestMetricsAccess.getMeters())
        .containsExactlyInAnyOrder(
            MeterId.from(CONNECTIONS_TOTAL, tags),
            MeterId.from(CONNECTIONS_ACTIVE, tags),
            MeterId.from(CONNECTIONS_IDLE, tags),
            MeterId.from(CONNECTIONS_MAX, tags));
  }

  private static void assertNoConnectionPoolMetrics() {
    assertThat(TestMetricsAccess.getMeterNames())
        .doesNotContain(
            CONNECTIONS_TOTAL.name(),
            CONNECTIONS_ACTIVE.name(),
            CONNECTIONS_IDLE.name(),
            CONNECTIONS_MAX.name());
  }
}
