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

import com.splunk.opentelemetry.instrumentation.jdbc.AbstractMySqlDbContextPropagator;
import io.opentelemetry.javaagent.bootstrap.CallDepth;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySqlTestUtil {

  public static String getTraceparent(
      Connection connection, Class<? extends AbstractMySqlDbContextPropagator> propagatorClass)
      throws SQLException {
    CallDepth callDepthSplunk = CallDepth.forClass(propagatorClass);
    CallDepth callDepthJdbc = CallDepth.forClass(Statement.class);
    // disable instrumentation, so we could read the current value
    callDepthSplunk.getAndIncrement();
    // disable jdbc instrumentation, so it wouldn't create a span for the statement execution
    callDepthJdbc.getAndIncrement();
    try (Statement statement = connection.createStatement()) {
      statement.execute("SELECT @traceparent");
      try (ResultSet resultSet = statement.getResultSet()) {
        resultSet.next();
        return resultSet.getString(1);
      }
    } finally {
      callDepthJdbc.decrementAndGet();
      callDepthSplunk.decrementAndGet();
    }
  }
}
