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

public class WildFlySmokeTest extends AppServerTest {

  public static final ExpectedServerAttributes WILDFLY_13_SERVER_ATTRIBUTES =
      new ExpectedServerAttributes("GET", "WildFly Full", "13.0.0.Final");
  public static final ExpectedServerAttributes WILDFLY_17_SERVER_ATTRIBUTES =
      new ExpectedServerAttributes("GET", "WildFly Full", "17.0.1.Final");
  public static final ExpectedServerAttributes WILDFLY_21_SERVER_ATTRIBUTES =
      new ExpectedServerAttributes("GET", "WildFly Full", "21.0.0.Final");
  public static final ExpectedServerAttributes WILDFLY_28_SERVER_ATTRIBUTES =
      new ExpectedServerAttributes("GET", "WildFly Full", "28.0.1.Final");

  private static Stream<Arguments> supportedConfigurations() {
    return configurations("wildfly")
        .otelLinux("13.0.0.Final", WILDFLY_13_SERVER_ATTRIBUTES, VMS_ALL, "8")
        .otelLinux("17.0.1.Final", WILDFLY_17_SERVER_ATTRIBUTES, VMS_ALL, "8", "11")
        .otelLinux("21.0.0.Final", WILDFLY_21_SERVER_ATTRIBUTES, VMS_ALL, "8", "11", "17", "21")
        .otelLinux("28.0.1.Final", WILDFLY_28_SERVER_ATTRIBUTES, VMS_ALL, "11", "17", "21")
        .otelWindows("13.0.0.Final", WILDFLY_13_SERVER_ATTRIBUTES, VMS_HOTSPOT, "8")
        .otelWindows("17.0.1.Final", WILDFLY_17_SERVER_ATTRIBUTES, VMS_HOTSPOT, "8", "11")
        .otelWindows("21.0.0.Final", WILDFLY_21_SERVER_ATTRIBUTES, VMS_HOTSPOT, "8", "11", "17", "21")
        .otelWindows("28.0.1.Final", WILDFLY_28_SERVER_ATTRIBUTES, VMS_HOTSPOT, "11", "17", "21")
        .stream();
  }

  @Override
  protected boolean shouldAutodetectServiceName() {
    return true;
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("supportedConfigurations")
  void wildflySmokeTest(TestImage image, ExpectedServerAttributes expectedServerAttributes)
      throws IOException, InterruptedException {
    startTargetOrSkipTest(image);

    assertServerHandler(expectedServerAttributes);
    assertWebAppTrace(expectedServerAttributes);

    stopTarget();
  }
}
