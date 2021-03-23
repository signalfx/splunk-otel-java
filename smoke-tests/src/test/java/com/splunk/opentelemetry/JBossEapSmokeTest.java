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
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * This is the smoke test for OpenTelemetry Java agent with JBoss EAP. The test is ignored if there
 * are no JBoss EAP images installed locally. See the manual in `matrix` sub-project for
 * instructions on how to build required images.
 */
public class JBossEapSmokeTest extends AppServerTest {

  public static final ExpectedServerAttributes JBOSS_EAP_7_1_SERVER_ATTRIBUTES =
      new ExpectedServerAttributes("HTTP GET", "JBoss EAP", "7.1.0.GA");
  public static final ExpectedServerAttributes JBOSS_EAP_7_3_SERVER_ATTRIBUTES =
      new ExpectedServerAttributes("HTTP GET", "JBoss EAP", "7.3.0.GA");

  private static Stream<Arguments> jboss() {
    return configurations("jboss-eap")
        .splunkLinux("7.1.0", JBOSS_EAP_7_1_SERVER_ATTRIBUTES, VMS_ALL, "8")
        .splunkLinux("7.3.0", JBOSS_EAP_7_3_SERVER_ATTRIBUTES, VMS_ALL, "8", "11").stream();
  }

  // TODO: this method can be removed after upstream javaagent 1.1.0 release
  // openj9 JDK8 does not have JFR classes, which causes all ComponentInstallers to run
  // synchronously, not after LogManager is loaded - see
  // AgentInstaller#installComponentsAfterByteBuddy()
  // micrometer's JvmGcMetrics references (indirectly) NotificationBroadcasterSupport which uses
  // ClassLogger which uses JUL and this causes JVM LogManager to load before the JBoss one
  // AFAIK only openj9 (IBM) JDK 8 has this problem, all other JDKs don't use JUL in MBeans
  @Override
  protected Map<String, String> getExtraEnv() {
    return Map.of("OTEL_INSTRUMENTATION_JVM_METRICS_ENABLED", "false");
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("jboss")
  void jbossSmokeTest(TestImage image, ExpectedServerAttributes expectedServerAttributes)
      throws IOException, InterruptedException {
    startTargetOrSkipTest(image);

    assertServerHandler(expectedServerAttributes);
    assertWebAppTrace(expectedServerAttributes);

    stopTarget();
  }
}
