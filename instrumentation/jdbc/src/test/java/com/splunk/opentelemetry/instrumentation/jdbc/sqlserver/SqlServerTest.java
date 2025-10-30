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

package com.splunk.opentelemetry.instrumentation.jdbc.sqlserver;

import com.splunk.opentelemetry.instrumentation.jdbc.AbstractDbContextPropagationTest;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.javaagent.bootstrap.CallDepth;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.mssqlserver.MSSQLServerContainer;

class SqlServerTest extends AbstractDbContextPropagationTest {
  private static final Logger logger = LoggerFactory.getLogger(SqlServerTest.class);

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  private static final MSSQLServerContainer sqlServer =
      new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

  static {
    sqlServer.withLogConsumer(new Slf4jLogConsumer(logger));
  }

  @BeforeAll
  static void setup() throws Exception {
    sqlServer.start();
    try (Connection connection =
        DriverManager.getConnection(sqlServer.getJdbcUrl(), "sa", sqlServer.getPassword())) {
      try (Statement createTable = connection.createStatement()) {
        createTable.execute("CREATE TABLE test_table (value INT NOT NULL)");
      }
    }

    testing.waitForTraces(1);
    testing.clearData();
  }

  @AfterAll
  static void cleanup() {
    sqlServer.stop();
  }

  @Override
  protected InstrumentationExtension getTesting() {
    return testing;
  }

  @Override
  protected Connection newConnection() throws SQLException {
    return DriverManager.getConnection(sqlServer.getJdbcUrl(), "sa", sqlServer.getPassword());
  }

  @Override
  protected String getTraceparent(Connection connection) throws SQLException {
    CallDepth callDepthSplunk = CallDepth.forClass(SqlServerContextPropagator.class);
    CallDepth callDepthJdbc = CallDepth.forClass(Statement.class);
    // disable instrumentation, so we could read the current value
    callDepthSplunk.getAndIncrement();
    // disable jdbc instrumentation, so it wouldn't create a span for the statement execution
    callDepthJdbc.getAndIncrement();
    try (Statement statement = connection.createStatement()) {
      statement.execute("SELECT CONTEXT_INFO()");
      try (ResultSet resultSet = statement.getResultSet()) {
        resultSet.next();
        byte[] bytes = resultSet.getBytes(1);
        return bytes == null ? null : toString(bytes);
      }
    } finally {
      callDepthJdbc.decrementAndGet();
      callDepthSplunk.decrementAndGet();
    }
  }

  private static String toString(byte[] bytes) {
    // CONTEXT_INFO() returns a 128 byte array that is padded with zeroes
    for (int i = 0; i < bytes.length; i++) {
      if (bytes[i] == 0) {
        return new String(bytes, 0, i, StandardCharsets.UTF_8);
      }
    }
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
