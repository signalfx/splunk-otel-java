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

import com.splunk.opentelemetry.helper.ResourceMapping;
import com.splunk.opentelemetry.helper.TargetWaitStrategy;
import com.splunk.opentelemetry.helper.TestImage;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class LibertySmokeTest extends AppServerTest {

  public static final ExpectedServerAttributes LIBERTY20_SERVER_ATTRIBUTES =
      new LibertyAttributes("20.0.0.12");
  public static final ExpectedServerAttributes LIBERTY21_SERVER_ATTRIBUTES =
      new LibertyAttributes("21.0.0.12");
  public static final ExpectedServerAttributes LIBERTY22_SERVER_ATTRIBUTES =
      new LibertyAttributes("22.0.0.12");
  public static final ExpectedServerAttributes LIBERTY23_SERVER_ATTRIBUTES =
      new LibertyAttributes("23.0.0.12");

  private static Stream<Arguments> supportedConfigurations() {
    return configurations("liberty")
        .otelLinux("20.0.0.12", LIBERTY20_SERVER_ATTRIBUTES, VMS_ALL, "8", "11")
        .otelLinux("21.0.0.12", LIBERTY21_SERVER_ATTRIBUTES, VMS_ALL, "8", "11", "17")
        .otelLinux("22.0.0.12", LIBERTY22_SERVER_ATTRIBUTES, VMS_ALL, "8", "11", "17")
        .otelLinux("23.0.0.12", LIBERTY23_SERVER_ATTRIBUTES, VMS_ALL, "8", "11", "17")
        .otelWindows("20.0.0.12", LIBERTY20_SERVER_ATTRIBUTES, VMS_HOTSPOT, "8", "11")
        .otelWindows("21.0.0.12", LIBERTY21_SERVER_ATTRIBUTES, VMS_HOTSPOT, "8", "11", "17")
        .otelWindows("22.0.0.12", LIBERTY22_SERVER_ATTRIBUTES, VMS_HOTSPOT, "8", "11", "17")
        .otelWindows("23.0.0.12", LIBERTY23_SERVER_ATTRIBUTES, VMS_HOTSPOT, "8", "11", "17")
        .stream();
  }

  @Override
  protected List<ResourceMapping> getExtraResources() {
    return List.of(
        // server.xml path on linux containers
        ResourceMapping.of("liberty-with-monitor.xml", "/config/server.xml"),
        // server.xml path on windows containers
        ResourceMapping.of(
            "liberty-with-monitor.xml", "/server/usr/servers/defaultServer/server.xml"));
  }

  @Override
  protected TargetWaitStrategy getWaitStrategy() {
    return new TargetWaitStrategy.Log(
        Duration.ofMinutes(3), ".*server is ready to run a smarter planet.*");
  }

  @Override
  protected boolean shouldAutodetectServiceName() {
    // test app isn't deployed through dropins directory
    return false;
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("supportedConfigurations")
  void libertySmokeTest(TestImage image, ExpectedServerAttributes expectedServerAttributes)
      throws IOException, InterruptedException {
    startTargetOrSkipTest(image);

    assertServerHandler(expectedServerAttributes);
    assertWebAppTrace(expectedServerAttributes);

    stopTarget();
  }

  public static class LibertyAttributes extends ExpectedServerAttributes {
    public LibertyAttributes(String version) {
      super("GET", "websphere liberty", version);
    }
  }
}
