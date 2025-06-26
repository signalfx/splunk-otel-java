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

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.javaagent.bootstrap.CallDepth;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

class SqlServerTest {
  private static final Logger logger = LoggerFactory.getLogger(SqlServerTest.class);

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  private static final MSSQLServerContainer sqlServer =
      new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();
  private static Connection connection;

  static {
    sqlServer.withLogConsumer(new Slf4jLogConsumer(logger));
  }

  @BeforeAll
  static void setup() throws Exception {
    sqlServer.start();
    connection = DriverManager.getConnection(sqlServer.getJdbcUrl(), "sa", sqlServer.getPassword());

    Statement createTable = connection.createStatement();
    createTable.execute("CREATE TABLE test_table (value INT NOT NULL)");
    createTable.close();

    testing.waitForTraces(1);
    testing.clearData();
  }

  @AfterAll
  static void cleanup() throws Exception {
    connection.close();
    sqlServer.stop();
  }

  private static List<Arguments> contextPropagationArguments() {
    return Arrays.asList(
        // Statement tests
        Arguments.of(
            (Action)
                connection -> {
                  try (Statement statement = connection.createStatement()) {
                    statement.executeQuery("SELECT 1");
                  }
                }),
        Arguments.of(
            (Action)
                connection -> {
                  try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("INSERT INTO test_table VALUES(1)");
                  }
                }),
        Arguments.of(
            (Action)
                connection -> {
                  try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(
                        "INSERT INTO test_table VALUES(1)", Statement.NO_GENERATED_KEYS);
                  }
                }),
        Arguments.of(
            (Action)
                connection -> {
                  try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("INSERT INTO test_table VALUES(1)", new int[] {1});
                  }
                }),
        Arguments.of(
            (Action)
                connection -> {
                  try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(
                        "INSERT INTO test_table VALUES(1)", new String[] {"value"});
                  }
                }),
        Arguments.of(
            (Action)
                connection -> {
                  try (Statement statement = connection.createStatement()) {
                    statement.execute("SELECT 1");
                  }
                }),
        Arguments.of(
            (Action)
                connection -> {
                  try (Statement statement = connection.createStatement()) {
                    statement.execute("SELECT 1", Statement.NO_GENERATED_KEYS);
                  }
                }),
        Arguments.of(
            (Action)
                connection -> {
                  try (Statement statement = connection.createStatement()) {
                    statement.execute(
                        "INSERT INTO test_table VALUES(1)", Statement.NO_GENERATED_KEYS);
                  }
                }),
        Arguments.of(
            (Action)
                connection -> {
                  try (Statement statement = connection.createStatement()) {
                    statement.execute("INSERT INTO test_table VALUES(1)", new int[] {1});
                  }
                }),
        Arguments.of(
            (Action)
                connection -> {
                  try (Statement statement = connection.createStatement()) {
                    statement.execute("INSERT INTO test_table VALUES(1)", new String[] {"value"});
                  }
                }),
        Arguments.of(
            (Action)
                connection -> {
                  try (Statement statement = connection.createStatement()) {
                    statement.addBatch("INSERT INTO test_table VALUES(1)");
                    statement.executeBatch();
                  }
                }),
        Arguments.of(
            (Action)
                connection -> {
                  try (Statement statement = connection.createStatement()) {
                    statement.addBatch("INSERT INTO test_table VALUES(1)");
                    statement.executeLargeBatch();
                  }
                }),
        Arguments.of(
            (Action)
                connection -> {
                  try (Statement statement = connection.createStatement()) {
                    statement.executeLargeUpdate("INSERT INTO test_table VALUES(1)");
                  }
                }),
        Arguments.of(
            (Action)
                connection -> {
                  try (Statement statement = connection.createStatement()) {
                    statement.executeLargeUpdate(
                        "INSERT INTO test_table VALUES(1)", Statement.NO_GENERATED_KEYS);
                  }
                }),
        Arguments.of(
            (Action)
                connection -> {
                  try (Statement statement = connection.createStatement()) {
                    statement.executeLargeUpdate("INSERT INTO test_table VALUES(1)", new int[] {1});
                  }
                }),
        Arguments.of(
            (Action)
                connection -> {
                  try (Statement statement = connection.createStatement()) {
                    statement.executeLargeUpdate(
                        "INSERT INTO test_table VALUES(1)", new String[] {"value"});
                  }
                }),
        // PreparedStatement tests
        Arguments.of(
            (Action)
                connection -> {
                  try (PreparedStatement statement = connection.prepareStatement("SELECT 1")) {
                    statement.executeQuery();
                  }
                }),
        Arguments.of(
            (Action)
                connection -> {
                  try (PreparedStatement statement =
                      connection.prepareStatement("INSERT INTO test_table VALUES(1)")) {
                    statement.executeUpdate();
                  }
                }),
        Arguments.of(
            (Action)
                connection -> {
                  try (PreparedStatement statement = connection.prepareStatement("SELECT 1")) {
                    statement.execute();
                  }
                }),
        Arguments.of(
            (Action)
                connection -> {
                  try (PreparedStatement statement =
                      connection.prepareStatement("INSERT INTO test_table VALUES(1)")) {
                    statement.executeLargeUpdate();
                  }
                }),
        Arguments.of(
            (Action)
                connection -> {
                  try (PreparedStatement statement =
                      connection.prepareStatement("INSERT INTO test_table VALUES(1)")) {
                    statement.addBatch();
                    statement.executeBatch();
                  }
                }),
        Arguments.of(
            (Action)
                connection -> {
                  try (PreparedStatement statement =
                      connection.prepareStatement("INSERT INTO test_table VALUES(1)")) {
                    statement.addBatch();
                    statement.executeLargeBatch();
                  }
                }),
        // CallableStatement tests
        Arguments.of(
            (Action)
                connection -> {
                  try (CallableStatement statement = connection.prepareCall("SELECT 1")) {
                    statement.execute();
                  }
                }));
  }

  @ParameterizedTest
  @MethodSource("contextPropagationArguments")
  void contextPropagation(Action action) throws Exception {
    Connection connection =
        DriverManager.getConnection(sqlServer.getJdbcUrl(), "sa", sqlServer.getPassword());

    assertNoContext(connection);

    testing.runWithSpan(
        "parent",
        () -> {
          action.accept(connection);
        });

    AtomicReference<SpanContext> jdbcSpan = new AtomicReference<>();
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent(),
                span -> {
                  span.hasParent(trace.getSpan(0));
                  jdbcSpan.set(span.actual().getSpanContext());
                }));

    assertSameSpan(jdbcSpan.get(), getContext(connection));

    assertNoContext(connection);
  }

  private static void assertSameSpan(SpanContext expected, Context context) {
    SpanContext actual = Span.fromContext(context).getSpanContext();
    assertThat(expected.getTraceId()).isEqualTo(actual.getTraceId());
    assertThat(expected.getSpanId()).isEqualTo(actual.getSpanId());
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

  private static Context toContext(String traceparent) {
    return W3CTraceContextPropagator.getInstance()
        .extract(
            Context.root(),
            traceparent,
            new TextMapGetter<>() {
              public String get(String carrier, String key) {
                if ("traceparent".equals(key)) {
                  return carrier;
                }
                return null;
              }

              @Override
              public Iterable<String> keys(String carrier) {
                return Collections.singleton("traceparent");
              }
            });
  }

  private static Context getContext(Connection connection) throws SQLException {
    CallDepth callDepthSplunk = CallDepth.forClass(SqlServerUtil.class);
    CallDepth callDepthJdbc = CallDepth.forClass(Statement.class);
    // disable instrumentation, so we could read the current value
    callDepthSplunk.getAndIncrement();
    // disable jdbc instrumentation, so it wouldn't create a span for the statement execution
    callDepthJdbc.getAndIncrement();
    try {
      Statement statement = connection.createStatement();
      statement.execute("SELECT CONTEXT_INFO()");
      ResultSet resultSet = statement.getResultSet();
      resultSet.next();
      byte[] bytes = resultSet.getBytes(1);
      return bytes != null ? toContext(toString(bytes)) : Context.root();
    } finally {
      callDepthJdbc.decrementAndGet();
      callDepthSplunk.decrementAndGet();
    }
  }

  private static void assertNoContext(Connection connection) throws SQLException {
    CallDepth callDepthJdbc = CallDepth.forClass(Statement.class);
    // disable jdbc instrumentation, so it wouldn't create a span for the statement execution
    callDepthJdbc.getAndIncrement();
    try {
      connection.createStatement().execute("SELECT 1");
      assertSameSpan(SpanContext.getInvalid(), getContext(connection));
    } finally {
      callDepthJdbc.decrementAndGet();
    }
  }

  @FunctionalInterface
  interface Action {
    void accept(Connection connection) throws Exception;
  }
}
