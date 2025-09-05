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

import com.splunk.opentelemetry.instrumentation.jdbc.AbstractDbContextPropagator;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class SqlServerContextPropagator extends AbstractDbContextPropagator {
  public static final SqlServerContextPropagator INSTANCE = new SqlServerContextPropagator();

  private SqlServerContextPropagator() {}

  @Override
  protected void setContext(Connection connection, String contextInfo) throws SQLException {
    byte[] contextBytes =
        contextInfo == null ? new byte[0] : contextInfo.getBytes(StandardCharsets.UTF_8);
    PreparedStatement statement = connection.prepareStatement("set context_info ?");
    statement.setBytes(1, contextBytes);
    statement.executeUpdate();
  }
}
