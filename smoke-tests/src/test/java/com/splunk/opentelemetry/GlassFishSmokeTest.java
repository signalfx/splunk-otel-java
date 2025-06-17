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
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class GlassFishSmokeTest extends AppServerTest {

  public static final ExpectedServerAttributes PAYARA20_SERVER_ATTRIBUTES =
      new ExpectedServerAttributes("GET /*", "Payara Server", "5.2020.6");
  public static final ExpectedServerAttributes PAYARA21_SERVER_ATTRIBUTES =
      new ExpectedServerAttributes("GET /*", "Payara Server", "5.2021.8");
  public static final ExpectedServerAttributes PAYARA23_SERVER_ATTRIBUTES =
      new ExpectedServerAttributes("GET /*", "Payara Server", "6.2023.12");

  private static Stream<Arguments> supportedConfigurations() {
    return configurations("payara")
        .otelLinux("5.2020.6", PAYARA20_SERVER_ATTRIBUTES, VMS_ALL, "8", "11")
        .otelLinux("5.2021.8", PAYARA21_SERVER_ATTRIBUTES, VMS_ALL, "8", "11")
        .otelLinux("6.2023.12", PAYARA23_SERVER_ATTRIBUTES, VMS_ALL, "17")
        .otelWindows("5.2020.6", PAYARA20_SERVER_ATTRIBUTES, VMS_ALL, "8", "11")
        .otelWindows("5.2021.8", PAYARA21_SERVER_ATTRIBUTES, VMS_ALL, "8", "11")
        .otelWindows("6.2023.12", PAYARA23_SERVER_ATTRIBUTES, VMS_ALL, "17", "21")
        .stream();
  }

  @Override
  protected String getJvmArgsEnvVarName() {
    return "JVM_ARGS";
  }

  @Override
  protected Map<String, String> getExtraEnv() {
    return Map.of("HZ_PHONE_HOME_ENABLED", "false");
  }

  @Override
  protected TargetWaitStrategy getWaitStrategy() {
    return new TargetWaitStrategy.Log(
        Duration.ofMinutes(5), ".*(app was successfully deployed|deployed with name app).*");
  }

  @Override
  protected boolean shouldAutodetectServiceName() {
    // payara in docker container is set up so that application is deployed only after payara has
    // been started, so we can't autodetect the service name based on deployed application
    return false;
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
