package com.splunk.opentelemetry.instrumentation.nocode;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;

// This test has "test/config/nocode.yml" applied to it by the gradle environment setting
public class NocodeInstrumentationTest {
  @RegisterExtension
  static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  @Test
  public void testBasicMethod() {
    new SampleClass().doSomething();
    // FIXME what is scope version verification and why doesn't it pass?
    testing.waitAndAssertTracesWithoutScopeVersionVerification(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("name")
                        .hasKind(SpanKind.INTERNAL)
                        .hasAttributesSatisfying(
                            equalTo(AttributeKey.stringKey("details"), "details"))));
  }

  @Test
  public void testRuleWithManyInvalidFields() {
    new SampleClass().doInvalidRule();
    testing.waitAndAssertTracesWithoutScopeVersionVerification(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("SampleClass.doInvalidRule")
                        .hasKind(SpanKind.INTERNAL)
                        .hasTotalAttributeCount(2))); // two code. attribute but nothing from the invalid rule
  }


  @Test
  public void testThrowException() {
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
                        .hasEventsSatisfyingExactly(
                            event ->
                                event.hasName("exception"))
                        .hasAttributesSatisfying(
                            equalTo(AttributeKey.stringKey("five"), "5"))));
  }

}
