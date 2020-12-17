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

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * This is the smoke test for OpenTelemetry Java agent with JBoss EAP. The test is ignored if there
 * are no JBoss EAP images installed locally. See the manual in `matrix` sub-project for
 * instructions on how to build required images.
 */
public class JBossEapSmokeTest extends ProprietaryAppServerTest {

  public static final ExpectedServerAttributes JBOSS_EAP_7_1_SERVER_ATTRIBUTES =
      new ExpectedServerAttributes("TODO", "JBoss EAP", "7.1.0.GA");
  public static final ExpectedServerAttributes JBOSS_EAP_7_3_SERVER_ATTRIBUTES =
      new ExpectedServerAttributes("TODO", "JBoss EAP", "7.3.0.GA");

  private static Stream<Arguments> jboss() {
    return Stream.of(
        arguments("splunk-jboss-eap:7.1.0-jdk8", JBOSS_EAP_7_1_SERVER_ATTRIBUTES),
        arguments("splunk-jboss-eap:7.3.0-jdk8", JBOSS_EAP_7_3_SERVER_ATTRIBUTES),
        arguments("splunk-jboss-eap:7.3.0-jdk11", JBOSS_EAP_7_3_SERVER_ATTRIBUTES));
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("jboss")
  void jbossSmokeTest(String imageName, ExpectedServerAttributes expectedServerAttributes)
      throws IOException, InterruptedException {
    assumeTrue(localDockerImageIsPresent(imageName));
    startTarget(imageName);

    // TODO support 404
    //    assertServerHandler(expectedServerAttributes);
    assertWebAppTrace(expectedServerAttributes);
  }
}
