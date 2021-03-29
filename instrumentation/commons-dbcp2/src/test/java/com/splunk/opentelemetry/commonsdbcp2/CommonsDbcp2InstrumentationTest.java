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

package com.splunk.opentelemetry.commonsdbcp2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

import com.splunk.opentelemetry.testing.TestMetricsAccess;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.Driver;
import java.util.concurrent.TimeUnit;
import javax.management.ObjectName;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(AgentInstrumentationExtension.class)
public class CommonsDbcp2InstrumentationTest {
  @Mock Driver driverMock;
  @Mock Connection connectionMock;

  @BeforeEach
  void setUpMocks() {}

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
        .untilAsserted(
            () ->
                assertThat(TestMetricsAccess.getMeterNames())
                    .containsExactlyInAnyOrder(
                        "db.pool.connections.active",
                        "db.pool.connections.idle",
                        "db.pool.connections.min",
                        "db.pool.connections.max"));
  }

  // Spring does not rely on BasicDataSource's built-in JMX registration - it registers each MXBean
  // that it finds in the context instead
  @Test
  void shouldReportMetricsForConnectionPool_springBootLikeScenario() throws Exception {
    // given
    var connectionPool = new BasicDataSource();
    connectionPool.setDriver(driverMock);
    connectionPool.setUrl("db:///url");

    // when
    ManagementFactory.getPlatformMBeanServer()
        .registerMBean(
            connectionPool,
            new ObjectName("org.apache.commons.dbcp2.BasicDataSource:name=dataSource"));

    // then
    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(
            () ->
                assertThat(TestMetricsAccess.getMeterNames())
                    .containsExactlyInAnyOrder(
                        "db.pool.connections.active",
                        "db.pool.connections.idle",
                        "db.pool.connections.min",
                        "db.pool.connections.max"));
  }
}
