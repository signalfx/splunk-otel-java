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

package com.splunk.opentelemetry;

import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.HashSet;
import java.util.Set;

public class JdbcSpanRenamingProcessor implements SpanProcessor {
  private static final Set<String> SQL_SYSTEMS;

  static {
    Set<String> sqlSystems = new HashSet<>();
    sqlSystems.add("db2");
    sqlSystems.add("derby");
    sqlSystems.add("h2");
    sqlSystems.add("hsqldb");
    sqlSystems.add("mariadb");
    sqlSystems.add("mssql");
    sqlSystems.add("mysql");
    sqlSystems.add("oracle");
    sqlSystems.add("other_sql");
    sqlSystems.add("postgresql");
    SQL_SYSTEMS = sqlSystems;
  }

  @Override
  public void onStart(ReadWriteSpan span) {
    AttributeValue dbSystem = span.toSpanData().getAttributes().get("db.system");
    if (isJdbcSpan(dbSystem)) {
      String query = span.getName().trim().toUpperCase();
      span.updateName(getStatementType(query));
    }
  }

  private boolean isJdbcSpan(AttributeValue dbSystem) {
    return dbSystem != null
        && dbSystem.getType() == AttributeValue.Type.STRING
        && SQL_SYSTEMS.contains(dbSystem.getStringValue());
  }

  private String getStatementType(String query) {
    if (query.startsWith("SELECT")) {
      return "SELECT";
    } else if (query.startsWith("UPDATE")) {
      return "UPDATE";
    } else if (query.startsWith("DELETE")) {
      return "DELETE";
    } else if (query.startsWith("INSERT")) {
      return "INSERT";
    } else if (query.startsWith("MERGE")) {
      return "MERGE";
    }

    return "JDBC";
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {}

  @Override
  public boolean isEndRequired() {
    return false;
  }

  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode forceFlush() {
    return CompletableResultCode.ofSuccess();
  }
}
