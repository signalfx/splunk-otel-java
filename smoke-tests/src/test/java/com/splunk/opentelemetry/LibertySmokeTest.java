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

public class LibertySmokeTest extends AppServerTest {

  public static final ExpectedServerAttributes LIBERTY20_SERVER_ATTRIBUTES =
      new LibertyAttributes("20.0.0.12");

  private static Stream<Arguments> supportedConfigurations() {
    return configurations("liberty")
        .otelLinux("20.0.0.12", LIBERTY20_SERVER_ATTRIBUTES, VMS_ALL, "8", "11", "15")
        .splunkWindows("20.0.0.12", LIBERTY20_SERVER_ATTRIBUTES, VMS_HOTSPOT, "8", "11", "15")
        .stream();
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
      super("HTTP GET", "websphere liberty", version);
    }
  }
}
