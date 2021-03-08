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

import com.splunk.opentelemetry.helper.TestImage;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class JettySmokeTest extends AppServerTest {

  public static final ExpectedServerAttributes JETTY9_SERVER_ATTRIBUTES =
      new ExpectedServerAttributes("HTTP GET", "jetty", "9.4.35.v20201120");
  public static final ExpectedServerAttributes JETTY10_SERVER_ATTRIBUTES =
      new ExpectedServerAttributes("HTTP GET", "jetty", "10.0.0");
  public static final ExpectedServerAttributes JETTY10_BETA_SERVER_ATTRIBUTES =
      new ExpectedServerAttributes("HTTP GET", "jetty", "10.0.0.beta3");

  private static Stream<Arguments> supportedConfigurations() {
    return configurations("jetty")
        .otelLinux("9.4.35", JETTY9_SERVER_ATTRIBUTES, VMS_ALL, "8", "11", "15")
        .otelLinux("10.0.0", JETTY10_SERVER_ATTRIBUTES, VMS_ALL, "11", "15")
        .splunkWindows("9.4.35", JETTY9_SERVER_ATTRIBUTES, VMS_ALL, "8", "11", "15")
        .splunkWindows("10.0.0", JETTY10_BETA_SERVER_ATTRIBUTES, VMS_ALL, "11", "15").stream();
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("supportedConfigurations")
  void jettySmokeTest(TestImage image, ExpectedServerAttributes expectedServerAttributes)
      throws IOException, InterruptedException {
    startTargetOrSkipTest(image);

    assertServerHandler(expectedServerAttributes);
    assertWebAppTrace(expectedServerAttributes);

    stopTarget();
  }
}
