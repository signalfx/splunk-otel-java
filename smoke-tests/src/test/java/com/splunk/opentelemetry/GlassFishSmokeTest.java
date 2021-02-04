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

import static com.splunk.opentelemetry.helper.TestImage.linuxImage;
import static com.splunk.opentelemetry.helper.TestImage.proprietaryWindowsImage;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.splunk.opentelemetry.helper.TargetWaitStrategy;
import com.splunk.opentelemetry.helper.TestImage;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class GlassFishSmokeTest extends AppServerTest {

  public static final ExpectedServerAttributes PAYARA_SERVER_ATTRIBUTES =
      new ExpectedServerAttributes(
          "/this-is-definitely-not-there-but-there-should-be-a-trace-nevertheless",
          "Payara Server",
          "5.2020.6");

  private static Stream<Arguments> supportedConfigurations() {
    return Stream.of(
        arguments(
            linuxImage(
                "ghcr.io/open-telemetry/java-test-containers:payara-5.2020.6-jdk11-jdk11-20201207.405832649"),
            PAYARA_SERVER_ATTRIBUTES),
        arguments(
            linuxImage(
                "ghcr.io/open-telemetry/java-test-containers:payara-5.2020.6-jdk8-20201207.405832649"),
            PAYARA_SERVER_ATTRIBUTES),
        arguments(
            proprietaryWindowsImage("splunk-payara:5.2020.6-jdk8-windows"),
            PAYARA_SERVER_ATTRIBUTES),
        arguments(
            proprietaryWindowsImage("splunk-payara:5.2020.6-jdk11-windows"),
            PAYARA_SERVER_ATTRIBUTES));
  }

  @Override
  protected Map<String, String> getExtraEnv() {
    return Map.of("HZ_PHONE_HOME_ENABLED", "false");
  }

  @Override
  protected TargetWaitStrategy getWaitStrategy() {
    return new TargetWaitStrategy.Log(Duration.ofMinutes(5), ".*app was successfully deployed.*");
  }

  @ParameterizedTest
  @MethodSource("supportedConfigurations")
  void payaraSmokeTest(TestImage image, ExpectedServerAttributes expectedServerAttributes)
      throws IOException, InterruptedException {
    startTargetOrSkipTest(image);

    assertServerHandler(expectedServerAttributes);
    assertWebAppTrace(expectedServerAttributes);

    stopTarget();
  }
}
