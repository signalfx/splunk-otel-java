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

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * This is the smoke test for OpenTelemetry Java agent with Oracle WebLogic. The test is ignored if
 * there are no WebLogic images installed locally. See the manual in src/weblogic directory for
 * instructions on how to build required images.
 */
class WebLogicSmokeTest extends AppServerTest {

  // FIXME: awaiting https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/1630
  private static final ExpectedServerAttributes WEBLOGIC_ATTRIBUTES =
      new ExpectedServerAttributes("", "", "");

  private static Stream<Arguments> supportedWlsConfigurations() {
    return Stream.of(
        arguments("splunk-weblogic:12.2.1.4-jdkdeveloper"),
        arguments("splunk-weblogic:14.1.1.0-jdkdeveloper-8"),
        arguments("splunk-weblogic:14.1.1.0-jdkdeveloper-11"));
  }

  @ParameterizedTest
  @MethodSource("supportedWlsConfigurations")
  public void webLogicSmokeTest(String imageName) throws IOException, InterruptedException {
    assumeTrue(
        localDockerImageIsPresent(imageName), "Local docker image " + imageName + " is present");

    startTarget(imageName);

    // FIXME: APMI-1300
    //    assertServerHandler(....)

    assertWebAppTrace(WEBLOGIC_ATTRIBUTES);

    stopTarget();
  }

  @Override
  protected void additionalWebAppTraceAssertions(
      TraceInspector traces, ExpectedServerAttributes serverAttributes) {
    Assertions.assertEquals(
        1, traces.countSpansByName("GreetingServlet.withSpan"), "Span for the annotated method");
  }

  @Override
  protected int totalNumberOfSpansInWebappTrace() {
    // test app in proprietary images also has one additional span for WithSpan annotation.
    return super.totalNumberOfSpansInWebappTrace() + 1;
  }

  @Override
  protected void assertMiddlewareAttributesInWebAppTrace(
      ExpectedServerAttributes serverAttributes, TraceInspector traces) {
    // FIXME: waiting for
    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/1630
  }
}
