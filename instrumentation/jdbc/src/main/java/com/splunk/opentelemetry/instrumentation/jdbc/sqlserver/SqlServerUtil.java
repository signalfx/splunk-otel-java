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

import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class SqlServerUtil {
  private static final VirtualField<Connection, String> connectionState =
      VirtualField.find(Connection.class, String.class);

  public static void propagateContext(Connection connection) throws SQLException {
    AtomicReference<String> state = new AtomicReference<>();
    W3CTraceContextPropagator.getInstance()
        .inject(
            Context.current(),
            state,
            (carrier, key, value) -> {
              if ("traceparent".equals(key)) {
                carrier.set(value);
              }
            });
    String traceparent = state.get();
    String existingTraceparent = connectionState.get(connection);
    if (traceparent == null && existingTraceparent != null) {
      // we need to clear existing tracing state from the connection
      connectionState.set(connection, null);
      PreparedStatement statement = connection.prepareStatement("set context_info 0x");
      statement.executeUpdate();
    } else if (!Objects.equals(traceparent, existingTraceparent)) {
      connectionState.set(connection, traceparent);
      PreparedStatement statement = connection.prepareStatement("set context_info ?");
      statement.setBytes(1, traceparent.getBytes(StandardCharsets.UTF_8));
      statement.executeUpdate();
    }
  }
}
