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

package com.splunk.opentelemetry.instrumentation.jdbc.mariadb;

import com.splunk.opentelemetry.instrumentation.jdbc.AbstractConnectionUsingDbContextPropagationTest;
import com.splunk.opentelemetry.instrumentation.jdbc.mysql.MySqlTestUtil;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.mariadb.MariaDBContainer;

class MariaDbTest extends AbstractConnectionUsingDbContextPropagationTest {
  private static final Logger logger = LoggerFactory.getLogger(MariaDbTest.class);

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  private static final MariaDBContainer server =
      new MariaDBContainer("mariadb:10.3.6").withLogConsumer(new Slf4jLogConsumer(logger));

  @BeforeAll
  static void setup() throws Exception {
    server.start();
    try (Connection connection =
        DriverManager.getConnection(
            server.getJdbcUrl(), server.getUsername(), server.getPassword())) {
      try (Statement createTable = connection.createStatement()) {
        createTable.execute("CREATE TABLE test_table (value INT NOT NULL)");
      }
      try (Statement create = connection.createStatement()) {
        create.execute("CREATE PROCEDURE nothing()" + "BEGIN " + "END;");
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
    return DriverManager.getConnection(
        server.getJdbcUrl(), server.getUsername(), server.getPassword());
  }

  @Override
  protected String getTraceparent(Connection connection) throws SQLException {
    return MySqlTestUtil.getTraceparent(connection);
  }
}
