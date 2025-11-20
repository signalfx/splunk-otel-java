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

package com.splunk.opentelemetry.instrumentation.jdbc.postgresql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.splunk.opentelemetry.instrumentation.jdbc.AbstractDbContextPropagationTest;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.postgresql.PostgreSQLContainer;

class PostgreSqlServerTest extends AbstractDbContextPropagationTest {
  private static final Logger logger = LoggerFactory.getLogger(PostgreSqlServerTest.class);

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  private static final PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:9.6.8")
          .withCommand("postgres -c log_statement=all")
          .withLogConsumer(outputFrame -> captureLog(outputFrame.getUtf8String()))
          .withLogConsumer(new Slf4jLogConsumer(logger));
  private static final List<String> executedSql = new ArrayList<>();

  @BeforeAll
  static void setup() throws Exception {
    postgres.start();
    try (Connection connection =
        DriverManager.getConnection(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
      try (Statement createTable = connection.createStatement()) {
        createTable.execute("CREATE TABLE test_table (value INT NOT NULL)");
      }
    }

    testing.waitForTraces(1);
    testing.clearData();
  }

  @AfterAll
  static void cleanup() {
    postgres.stop();
  }

  @BeforeEach
  void cleanupTest() {
    executedSql.clear();
  }

  private static void captureLog(String log) {
    String prefix = "LOG:  execute <unnamed>: ";
    if (!log.startsWith(prefix)) {
      return;
    }
    String sql = log.substring(prefix.length()).trim();
    executedSql.add(sql);
  }

  @Override
  protected InstrumentationExtension getTesting() {
    return testing;
  }

  @Override
  protected boolean supportsColumnIndexes() {
    return false;
  }

  @Override
  protected boolean supportsLarge() {
    return Boolean.getBoolean("testLatestDeps");
  }

  @Override
  protected Connection newConnection() throws SQLException {
    return DriverManager.getConnection(
        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
  }

  @Override
  protected void assertBeforeQuery(Connection connection) {}

  @Override
  protected void assertAfterQuery(Connection connection, SpanContext parent, SpanContext jdbc) {
    String expectedComment =
        String.format(
            "/*service.name='test-service', traceparent='00-%s-%s-01'",
            parent.getTraceId(), parent.getSpanId());
    await()
        .untilAsserted(
            () -> assertThat(executedSql).anyMatch(sql -> sql.contains(expectedComment)));
  }
}
