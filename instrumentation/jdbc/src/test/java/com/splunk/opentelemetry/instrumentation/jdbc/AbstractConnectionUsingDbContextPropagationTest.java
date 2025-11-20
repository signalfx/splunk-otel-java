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

package com.splunk.opentelemetry.instrumentation.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.javaagent.bootstrap.CallDepth;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

public abstract class AbstractConnectionUsingDbContextPropagationTest
    extends AbstractDbContextPropagationTest {

  @Override
  protected void assertBeforeQuery(Connection connection) throws Exception {
    assertNoContext(connection);
  }

  @Override
  protected void assertAfterQuery(Connection connection, SpanContext parent, SpanContext jdbc)
      throws Exception {
    assertSameSpan(jdbc, getContext(connection));
    assertNoContext(connection);
  }

  private static void assertSameSpan(SpanContext expected, Context context) {
    SpanContext actual = Span.fromContext(context).getSpanContext();
    assertThat(expected.getTraceId()).isEqualTo(actual.getTraceId());
    assertThat(expected.getSpanId()).isEqualTo(actual.getSpanId());
  }

  private static Context toContext(String traceparent) {
    if (traceparent == null) {
      return Context.root();
    }

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

  protected abstract String getTraceparent(Connection connection) throws SQLException;

  private Context getContext(Connection connection) throws SQLException {
    return toContext(getTraceparent(connection));
  }

  private void assertNoContext(Connection connection) throws SQLException {
    CallDepth callDepthJdbc = CallDepth.forClass(Statement.class);
    // disable jdbc instrumentation, so it wouldn't create a span for the statement execution
    callDepthJdbc.getAndIncrement();
    try (Statement statement = connection.createStatement()) {
      statement.execute("SELECT 1");
      assertSameSpan(SpanContext.getInvalid(), getContext(connection));
    } finally {
      callDepthJdbc.decrementAndGet();
    }
  }
}
