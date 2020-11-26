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

public class JettySmokeTest extends AppServerTest {

  public static final ExpectedServerAttributes JETTY9_SERVER_ATTRIBUTES =
      new ExpectedServerAttributes("HandlerCollection.handle", "jetty", "9.4.35.v20201120");
  public static final ExpectedServerAttributes JETTY10_SERVER_ATTRIBUTES =
      new ExpectedServerAttributes("HandlerList.handle", "jetty", "10.0.0.beta3");

  private static Stream<Arguments> supportedConfigurations() {
    return Stream.of(
        arguments("splunk-jetty:9.4-jdk8", JETTY9_SERVER_ATTRIBUTES),
        arguments("splunk-jetty:9.4-jdk11", JETTY9_SERVER_ATTRIBUTES),
        arguments("splunk-jetty:9.4-jdk15", JETTY9_SERVER_ATTRIBUTES),
        arguments("splunk-jetty:10.0.0.beta3-jdk11", JETTY10_SERVER_ATTRIBUTES),
        arguments("splunk-jetty:10.0.0.beta3-jdk15", JETTY10_SERVER_ATTRIBUTES));
  }

  @ParameterizedTest
  @MethodSource("supportedConfigurations")
  void jettySmokeTest(String imageName, ExpectedServerAttributes expectedServerAttributes)
      throws IOException, InterruptedException {
    startTarget(imageName);

    assertServerHandler(expectedServerAttributes);
    assertWebAppTrace(expectedServerAttributes);
  }
}
