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

import static org.junit.jupiter.params.provider.Arguments.arguments;

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
    return Stream.of(
        arguments("splunk-tomcat:7-jdk8", TOMCAT7_SERVER_ATTRIBUTES),
        arguments("splunk-tomcat:8-jdk8", TOMCAT8_SERVER_ATTRIBUTES),
        arguments("splunk-tomcat:8-jdk11", TOMCAT8_SERVER_ATTRIBUTES),
        arguments("splunk-tomcat:9-jdk8", TOMCAT9_SERVER_ATTRIBUTES),
        arguments("splunk-tomcat:9-jdk11", TOMCAT9_SERVER_ATTRIBUTES));
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("supportedConfigurations")
  void jettySmokeTest(String imageName, ExpectedServerAttributes expectedServerAttributes)
      throws IOException, InterruptedException {
    startTarget(imageName);

    assertServerHandler(expectedServerAttributes);
    assertWebAppTrace(expectedServerAttributes);
  }

  public static class TomcatAttributes extends ExpectedServerAttributes {
    public TomcatAttributes(String version) {
      super(
          "/this-is-definitely-not-there-but-there-should-be-a-trace-nevertheless",
          "tomcat",
          version);
    }
  }
}
