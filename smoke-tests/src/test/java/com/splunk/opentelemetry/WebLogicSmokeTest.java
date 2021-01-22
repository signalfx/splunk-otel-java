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

import static com.splunk.opentelemetry.helper.TestImage.proprietaryLinuxImage;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.splunk.opentelemetry.helper.TestImage;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * This is the smoke test for OpenTelemetry Java agent with Oracle WebLogic. The test is ignored if
 * there are no WebLogic images installed locally. See the manual in `matrix` sub-project for
 * instructions on how to build required images.
 */
class WebLogicSmokeTest extends AppServerTest {

  // FIXME: awaiting https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/1630
  private static final AppServerTest.ExpectedServerAttributes WEBLOGIC_ATTRIBUTES =
      new AppServerTest.ExpectedServerAttributes("", "", "");

  private static Stream<Arguments> supportedWlsConfigurations() {
    return Stream.of(
        arguments(proprietaryLinuxImage("splunk-weblogic:12.1.3-jdkdeveloper")),
        arguments(proprietaryLinuxImage("splunk-weblogic:12.2.1.4-jdkdeveloper")),
        arguments(proprietaryLinuxImage("splunk-weblogic:14.1.1.0-jdkdeveloper-8")),
        arguments(proprietaryLinuxImage("splunk-weblogic:14.1.1.0-jdkdeveloper-11")));
  }

  @ParameterizedTest
  @MethodSource("supportedWlsConfigurations")
  public void webLogicSmokeTest(TestImage image) throws IOException, InterruptedException {
    startTargetOrSkipTest(image);

    // FIXME: APMI-1300
    //    assertServerHandler(....)

    assertWebAppTrace(WEBLOGIC_ATTRIBUTES, image);

    stopTarget();
  }

  @Override
  protected void assertMiddlewareAttributesInWebAppTrace(
      ExpectedServerAttributes serverAttributes, TraceInspector traces) {
    // FIXME: waiting for
    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/1630
  }
}
