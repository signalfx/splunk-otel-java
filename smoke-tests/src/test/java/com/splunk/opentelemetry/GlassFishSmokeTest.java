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
import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

public class GlassFishSmokeTest extends AppServerTest {

  public static final ExpectedServerAttributes PAYARA_SERVER_ATTRIBUTES =
      new ExpectedServerAttributes(
          "/this-is-definitely-not-there-but-there-should-be-a-trace-nevertheless",
          "Payara Server",
          "5.2020.6");

  private static Stream<Arguments> supportedConfigurations() {
    return Stream.of(
        arguments(
            "ghcr.io/open-telemetry/java-test-containers:payara-5.2020.6-jdk11-jdk11-20201207.405832649",
            PAYARA_SERVER_ATTRIBUTES),
        arguments(
            "ghcr.io/open-telemetry/java-test-containers:payara-5.2020.6-jdk8-20201207.405832649",
            PAYARA_SERVER_ATTRIBUTES));
  }

  @Override
  protected Map<String, String> getExtraEnv() {
    return Map.of("HZ_PHONE_HOME_ENABLED", "false");
  }

  @Override
  protected WaitStrategy getWaitStrategy() {
    return Wait.forLogMessage(".*app was successfully deployed.*", 1)
        .withStartupTimeout(Duration.ofMinutes(15));
  }

  @ParameterizedTest
  @MethodSource("supportedConfigurations")
  void payaraSmokeTest(String imageName, ExpectedServerAttributes expectedServerAttributes)
      throws IOException, InterruptedException {
    startTarget(imageName);

    assertServerHandler(expectedServerAttributes);
    assertWebAppTrace(expectedServerAttributes);
  }
}
