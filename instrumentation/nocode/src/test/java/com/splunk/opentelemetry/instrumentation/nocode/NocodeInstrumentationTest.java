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

package com.splunk.opentelemetry.instrumentation.nocode;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

// This test has "test/config/nocode.yml" applied to it by the gradle environment setting
class NocodeInstrumentationTest {
  @RegisterExtension
  static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  @Test
  void testBasicMethod() {
    new SampleClass().doSomething();
    // FIXME what is scope version verification and why doesn't it pass?
    testing.waitAndAssertTracesWithoutScopeVersionVerification(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("name")
                        .hasKind(SpanKind.INTERNAL)
                        .hasAttributesSatisfying(
                            equalTo(AttributeKey.stringKey("map.size"), "2"),
                            equalTo(AttributeKey.stringKey("details"), "details"))));
  }

  @Test
  void testRuleWithManyInvalidFields() {
    new SampleClass().doInvalidRule();
    testing.waitAndAssertTracesWithoutScopeVersionVerification(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("SampleClass.doInvalidRule")
                        .hasKind(SpanKind.INTERNAL)
                        .hasTotalAttributeCount(
                            2))); // two code. attribute but nothing from the invalid rule
  }

  @Test
  void testThrowException() {
    try {
      new SampleClass().throwException(5);
    } catch (Exception expected) {
      // nop
    }

    testing.waitAndAssertTracesWithoutScopeVersionVerification(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("SampleClass.throwException")
                        .hasKind(SpanKind.SERVER)
                        .hasEventsSatisfyingExactly(event -> event.hasName("exception"))
                        .hasAttributesSatisfying(equalTo(AttributeKey.stringKey("five"), "5"))));
  }

  public static class SampleClass {
    public String getName() {
      return "name";
    }

    public String getDetails() {
      return "details";
    }

    public Map<String, String> getMap() {
      HashMap<String, String> answer = new HashMap<>();
      answer.put("key", "value");
      answer.put("key2", "value2");
      return answer;
    }

    public void throwException(int parameter) {
      throw new UnsupportedOperationException("oh no");
    }

    public void doSomething() {}

    public void doInvalidRule() {}
  }
}
