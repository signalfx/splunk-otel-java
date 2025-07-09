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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class WebSphereSmokeTest extends AppServerTest {

  public static final ExpectedServerAttributes WEBSPHERE8_SERVER_ATTRIBUTES =
      new WebSphereAttributes("8.5.5.22");
  public static final ExpectedServerAttributes WEBSPHERE9_SERVER_ATTRIBUTES =
      new WebSphereAttributes("9.0.5.14");

  private static Stream<Arguments> supportedConfigurations() {
    return configurations("websphere")
        .otelLinux("8.5.5.22", WEBSPHERE8_SERVER_ATTRIBUTES, VMS_OPENJ9, "8")
        .otelLinux("9.0.5.14", WEBSPHERE9_SERVER_ATTRIBUTES, VMS_OPENJ9, "8")
        .stream();
  }

  @Override
  protected TargetWaitStrategy getWaitStrategy() {
    return new TargetWaitStrategy.Log(
        Duration.ofMinutes(3), ".*Server server1 open for e-business.*");
  }

  @Override
  protected boolean shouldAutodetectServiceName() {
    return true;
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("supportedConfigurations")
  void webSphereSmokeTest(TestImage image, ExpectedServerAttributes expectedServerAttributes)
      throws IOException, InterruptedException {
    startTargetOrSkipTest(image);

    // server handler test not implemented
    assertWebAppTrace(expectedServerAttributes);

    stopTarget();
  }

  public static class WebSphereAttributes extends ExpectedServerAttributes {
    public WebSphereAttributes(String version) {
      super("HTTP GET", "websphere", version);
    }
  }
}
