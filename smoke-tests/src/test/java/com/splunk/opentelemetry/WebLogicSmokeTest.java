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

import com.splunk.opentelemetry.helper.TargetWaitStrategy;
import com.splunk.opentelemetry.helper.TestImage;
import java.io.IOException;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * This is the smoke test for OpenTelemetry Java agent with Oracle WebLogic. The test is ignored if
 * there are no WebLogic images installed locally. See the manual in `matrix` sub-project for
 * instructions on how to build required images.
 */
class WebLogicSmokeTest extends AppServerTest {

  private static final AppServerTest.ExpectedServerAttributes V12_1_ATTRIBUTES =
      new AppServerTest.ExpectedServerAttributes("GET", "WebLogic Server", "12.1.3.0.0");
  private static final AppServerTest.ExpectedServerAttributes V12_2_ATTRIBUTES =
      new AppServerTest.ExpectedServerAttributes("GET", "WebLogic Server", "12.2.1.4.0");
  private static final AppServerTest.ExpectedServerAttributes V14_ATTRIBUTES =
      new AppServerTest.ExpectedServerAttributes("GET", "WebLogic Server", "14.1.1.0.0");

  private static Stream<Arguments> supportedWlsConfigurations() {
    return configurations("weblogic")
        .splunkLinux("12.1.3", V12_1_ATTRIBUTES, VMS_HOTSPOT, "8")
        .splunkLinux("12.2.1.4", V12_2_ATTRIBUTES, VMS_HOTSPOT, "8")
        .splunkLinux("14.1.1.0", V14_ATTRIBUTES, VMS_HOTSPOT, "8", "11")
        .stream();
  }

  @Override
  protected TargetWaitStrategy getWaitStrategy() {
    return new TargetWaitStrategy.Log(
        Duration.ofMinutes(5), ".*<Server state changed to RUNNING.>.*");
  }

  @ParameterizedTest
  @MethodSource("supportedWlsConfigurations")
  public void webLogicSmokeTest(TestImage image, ExpectedServerAttributes serverAttributes)
      throws IOException, InterruptedException {
    startTargetOrSkipTest(image);

    // No assertServerHandler as there are no current plans to have a WebLogic server handler that
    // creates spans
    assertWebAppTrace(serverAttributes);

    stopTarget();
  }

  @Override
  protected void assertWebengineAttributesInWebAppTrace(
      ExpectedServerAttributes serverAttributes, TraceInspector traces) {
    super.assertWebengineAttributesInWebAppTrace(serverAttributes, traces);

    Assertions.assertEquals(
        "domain1",
        traces.getServerSpanAttribute("webengine.weblogic.domain"),
        "WebLogic Domain attribute present");

    Assertions.assertEquals(
        "admin-server",
        traces.getServerSpanAttribute("webengine.weblogic.server"),
        "WebLogic Server attribute present");

    Assertions.assertEquals(
        "app",
        traces.getServerSpanAttribute("webengine.weblogic.application"),
        "WebLogic Application attribute present");
  }
}
