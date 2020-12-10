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

public class WildFlySmokeTest extends AppServerTest {

  public static final ExpectedServerAttributes WILDFLY_13_SERVER_ATTRIBUTES =
      new ExpectedServerAttributes(
          "/this-is-definitely-not-there-but-there-should-be-a-trace-nevertheless",
          "WildFly Full",
          "13.0.0.Final");
  public static final ExpectedServerAttributes WILDFLY_17_SERVER_ATTRIBUTES =
      new ExpectedServerAttributes(
          "/this-is-definitely-not-there-but-there-should-be-a-trace-nevertheless",
          "WildFly Full",
          "17.0.1.Final");
  public static final ExpectedServerAttributes WILDFLY_21_SERVER_ATTRIBUTES =
      new ExpectedServerAttributes(
          "/this-is-definitely-not-there-but-there-should-be-a-trace-nevertheless",
          "WildFly Full",
          "21.0.0.Final");

  private static Stream<Arguments> supportedConfigurations() {
    return Stream.of(
        arguments(
            "ghcr.io/open-telemetry/java-test-containers:wildfly-13.0.0.Final-jdk8-20201207.405832649",
            WILDFLY_13_SERVER_ATTRIBUTES),
        arguments(
            "ghcr.io/open-telemetry/java-test-containers:wildfly-17.0.1.Final-jdk8-20201207.405832649",
            WILDFLY_17_SERVER_ATTRIBUTES),
        arguments(
            "ghcr.io/open-telemetry/java-test-containers:wildfly-17.0.1.Final-jdk11-20201207.405832649",
            WILDFLY_17_SERVER_ATTRIBUTES),
        arguments(
            "ghcr.io/open-telemetry/java-test-containers:wildfly-21.0.0.Final-jdk8-20201207.405832649",
            WILDFLY_21_SERVER_ATTRIBUTES),
        arguments(
            "ghcr.io/open-telemetry/java-test-containers:wildfly-21.0.0.Final-jdk11-20201207.405832649",
            WILDFLY_21_SERVER_ATTRIBUTES));
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("supportedConfigurations")
  void wildflySmokeTest(String imageName, ExpectedServerAttributes expectedServerAttributes)
      throws IOException, InterruptedException {
    startTarget(imageName);

    // TODO support 404
    //    assertServerHandler(expectedServerAttributes);
    assertWebAppTrace(expectedServerAttributes);
  }
}
