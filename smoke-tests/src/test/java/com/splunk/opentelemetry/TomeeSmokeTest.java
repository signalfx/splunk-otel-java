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

public class TomeeSmokeTest extends AppServerTest {

  public static final ExpectedServerAttributes TOMEE7_SERVER_ATTRIBUTES =
      new TomeeAttributes("7.0.9");
  public static final ExpectedServerAttributes TOMEE8_SERVER_ATTRIBUTES =
      new TomeeAttributes("8.0.16");

  private static Stream<Arguments> supportedConfigurations() {
    return configurations("tomee")
        .otelLinux("7.0.9", TOMEE7_SERVER_ATTRIBUTES, VMS_ALL, "8")
        .otelLinux("8.0.16", TOMEE8_SERVER_ATTRIBUTES, VMS_ALL, "8", "11", "17", "21")
        .otelWindows("7.0.9", TOMEE7_SERVER_ATTRIBUTES, VMS_HOTSPOT, "8")
        .otelWindows("8.0.16", TOMEE8_SERVER_ATTRIBUTES, VMS_HOTSPOT, "8", "11", "17", "21")
        .stream();
  }

  @Override
  protected boolean shouldAutodetectServiceName() {
    return true;
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("supportedConfigurations")
  void tomeeSmokeTest(TestImage image, ExpectedServerAttributes expectedServerAttributes)
      throws IOException, InterruptedException {
    startTargetOrSkipTest(image);

    assertServerHandler(expectedServerAttributes);
    assertWebAppTrace(expectedServerAttributes);

    stopTarget();
  }

  public static class TomeeAttributes extends ExpectedServerAttributes {
    public TomeeAttributes(String version) {
      // This handler span name is only received if default webapps are present
      super("GET /*", "tomee", version);
    }
  }
}
