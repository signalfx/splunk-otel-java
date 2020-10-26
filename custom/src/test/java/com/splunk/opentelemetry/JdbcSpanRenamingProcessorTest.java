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

import static io.opentelemetry.trace.attributes.SemanticAttributes.DB_SYSTEM;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JdbcSpanRenamingProcessorTest {
  @Mock(answer = RETURNS_DEEP_STUBS)
  ReadWriteSpan span;

  SpanProcessor processor = new JdbcSpanRenamingProcessor();

  @Test
  void shouldRequireOnStart() {
    assertTrue(processor.isStartRequired());
  }

  @Test
  void shouldIgnoreSpansThatDoNotHaveDbSystemAttribute() {
    // given
    given(span.toSpanData().getAttributes().get(DB_SYSTEM)).willReturn(null);

    // when
    processor.onStart(span);

    // then
    then(span).should(never()).updateName(anyString());
  }

  @Test
  void shouldIgnoreSpansThatAreNotSql() {
    // given
    given(span.toSpanData().getAttributes().get(DB_SYSTEM)).willReturn("mongodb");

    // when
    processor.onStart(span);

    // then
    then(span).should(never()).updateName(anyString());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "db2",
        "derby",
        "h2",
        "hsqldb",
        "mariadb",
        "mssql",
        "mysql",
        "oracle",
        "other_sql",
        "postgresql"
      })
  void shouldUpdateJdbcSpans(String dbSystem) {
    // given
    given(span.toSpanData().getAttributes().get(DB_SYSTEM)).willReturn(dbSystem);
    given(span.getName()).willReturn("select * from table");

    // when
    processor.onStart(span);

    // then
    then(span).should().updateName("SELECT");
  }

  @ParameterizedTest
  @MethodSource("spanNameArgs")
  void shouldGetSqlStatementTypeFromSpanName(String query, String sqlStatementType) {
    // given
    given(span.toSpanData().getAttributes().get(DB_SYSTEM)).willReturn("other_sql");
    given(span.getName()).willReturn(query);

    // when
    processor.onStart(span);

    // then
    then(span).should().updateName(sqlStatementType);
  }

  private static Stream<Arguments> spanNameArgs() {
    return Stream.of(
        arguments(" Select * From Table", "SELECT"),
        arguments("  \tINSert into TAble", "INSERT"),
        arguments(" UPDATE table", "UPDATE"),
        arguments("delete from table", "DELETE"),
        arguments(" merge into table", "MERGE"),
        arguments("alter table table add column", "JDBC"));
  }
}
