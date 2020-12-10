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

public class LibertySmokeTest extends AppServerTest {

  public static final ExpectedServerAttributes LIBERTY20_SERVER_ATTRIBUTES =
      new LibertyAttributes("20.0.0.12");

  private static Stream<Arguments> supportedConfigurations() {
    return Stream.of(
        arguments(
            "ghcr.io/open-telemetry/java-test-containers:liberty-20.0.0.12-jdk8-20201209.410207048",
            LIBERTY20_SERVER_ATTRIBUTES),
        arguments(
            "ghcr.io/open-telemetry/java-test-containers:liberty-20.0.0.12-jdk11-20201209.410207048",
            LIBERTY20_SERVER_ATTRIBUTES),
        arguments(
            "ghcr.io/open-telemetry/java-test-containers:liberty-20.0.0.12-jdk15-20201209.410207048",
            LIBERTY20_SERVER_ATTRIBUTES),
        arguments(
            "ghcr.io/open-telemetry/java-test-containers:liberty-20.0.0.12-jdk8-jdk-openj9-20201209.410207048",
            LIBERTY20_SERVER_ATTRIBUTES),
        arguments(
            "ghcr.io/open-telemetry/java-test-containers:liberty-20.0.0.12-jdk11-jdk-openj9-20201209.410207048",
            LIBERTY20_SERVER_ATTRIBUTES),
        arguments(
            "ghcr.io/open-telemetry/java-test-containers:liberty-20.0.0.12-jdk15-jdk-openj9-20201209.410207048",
            LIBERTY20_SERVER_ATTRIBUTES));
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("supportedConfigurations")
  void libertySmokeTest(String imageName, ExpectedServerAttributes expectedServerAttributes)
      throws IOException, InterruptedException {
    startTarget(imageName);

    // TODO: not implemented
    //    assertServerHandler(expectedServerAttributes);
    assertWebAppTrace(expectedServerAttributes);
  }

  public static class LibertyAttributes extends ExpectedServerAttributes {
    public LibertyAttributes(String version) {
      super(
          "/this-is-definitely-not-there-but-there-should-be-a-trace-nevertheless",
          "websphere liberty",
          version);
    }
  }
}
