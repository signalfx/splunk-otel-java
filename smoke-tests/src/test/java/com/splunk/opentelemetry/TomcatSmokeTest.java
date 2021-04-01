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

public class TomcatSmokeTest extends AppServerTest {

  public static final ExpectedServerAttributes TOMCAT7_SERVER_ATTRIBUTES =
      new TomcatAttributes("7.0.107.0");
  public static final ExpectedServerAttributes TOMCAT8_SERVER_ATTRIBUTES =
      new TomcatAttributes("8.5.60.0");
  public static final ExpectedServerAttributes TOMCAT9_SERVER_ATTRIBUTES =
      new TomcatAttributes("9.0.40.0");

  private static Stream<Arguments> supportedConfigurations() {
    return configurations("tomcat")
        .otelLinux("7.0.107", TOMCAT7_SERVER_ATTRIBUTES, VMS_ALL, "8")
        .otelLinux("8.5.60", TOMCAT8_SERVER_ATTRIBUTES, VMS_ALL, "8", "11")
        .otelLinux("9.0.40", TOMCAT9_SERVER_ATTRIBUTES, VMS_ALL, "8", "11")
        .otelWindows("7.0.107", TOMCAT7_SERVER_ATTRIBUTES, VMS_ALL, "8")
        .otelWindows("8.5.60", TOMCAT8_SERVER_ATTRIBUTES, VMS_ALL, "8", "11")
        .otelWindows("9.0.40", TOMCAT9_SERVER_ATTRIBUTES, VMS_ALL, "8", "11")
        .stream();
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("supportedConfigurations")
  void tomcatSmokeTest(TestImage image, ExpectedServerAttributes expectedServerAttributes)
      throws IOException, InterruptedException {
    startTargetOrSkipTest(image);

    assertServerHandler(expectedServerAttributes);
    assertWebAppTrace(expectedServerAttributes);

    stopTarget();
  }

  public static class TomcatAttributes extends ExpectedServerAttributes {
    public TomcatAttributes(String version) {
      // This handler span name is only received if default webapps are removed
      super("HTTP GET", "tomcat", version);
    }
  }
}
