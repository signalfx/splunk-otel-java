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

package com.splunk.opentelemetry.instrumentation.jdbc.mysql;

import com.splunk.opentelemetry.instrumentation.jdbc.AbstractConnectionUsingDbContextPropagationTest;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.mysql.MySQLContainer;

class MySqlTest extends AbstractConnectionUsingDbContextPropagationTest {
  private static final Logger logger = LoggerFactory.getLogger(MySqlTest.class);

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  private static final MySQLContainer server =
      new MySQLContainer("mysql:8.0.32").withLogConsumer(new Slf4jLogConsumer(logger));

  @BeforeAll
  static void setup() throws Exception {
    server.start();
    try (Connection connection =
        DriverManager.getConnection(
            server.getJdbcUrl(), server.getUsername(), server.getPassword())) {
      try (Statement createTable = connection.createStatement()) {
        createTable.execute("CREATE TABLE test_table (value INT NOT NULL)");
      }
    }

    testing.waitForTraces(1);
    testing.clearData();
  }

  @AfterAll
  static void cleanup() {
    server.stop();
  }

  @Override
  protected InstrumentationExtension getTesting() {
    return testing;
  }

  @Override
  protected Connection newConnection() throws SQLException {
    Properties properties = new Properties();
    properties.put("user", server.getUsername());
    properties.put("password", server.getPassword());
    // disable tracing built into mysql driver
    properties.put("openTelemetry", "DISABLED");
    return DriverManager.getConnection(server.getJdbcUrl(), properties);
  }

  @Override
  protected String getTraceparent(Connection connection) throws SQLException {
    return MySqlTestUtil.getTraceparent(connection);
  }
}
