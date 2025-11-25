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

import com.splunk.opentelemetry.instrumentation.jdbc.AbstractConnectionUsingDbContextPropagationTest;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.javaagent.bootstrap.CallDepth;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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

class PostgreSqlTest extends AbstractConnectionUsingDbContextPropagationTest {
  private static final Logger logger = LoggerFactory.getLogger(PostgreSqlTest.class);

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
  protected String getTraceparent(Connection connection) throws SQLException {
    CallDepth callDepthSplunk = CallDepth.forClass(PostgreSqlContextPropagator.class);
    CallDepth callDepthJdbc = CallDepth.forClass(Statement.class);
    // disable instrumentation, so we could read the current value
    callDepthSplunk.getAndIncrement();
    // disable jdbc instrumentation, so it wouldn't create a span for the statement execution
    callDepthJdbc.getAndIncrement();
    try (Statement statement = connection.createStatement()) {
      statement.execute("SELECT CURRENT_SETTING('application_name')");
      try (ResultSet resultSet = statement.getResultSet()) {
        resultSet.next();
        return resultSet.getString(1);
      }
    } finally {
      callDepthJdbc.decrementAndGet();
      callDepthSplunk.decrementAndGet();
    }
  }

  @Override
  protected void assertAfterQuery(Connection connection, SpanContext parent, SpanContext jdbc)
      throws Exception {
    super.assertAfterQuery(connection, parent, jdbc);

    String expectedComment = "/*service.name='test-service'";
    await()
        .untilAsserted(
            () -> assertThat(executedSql).anyMatch(sql -> sql.contains(expectedComment)));
  }
}
