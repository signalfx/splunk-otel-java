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
import org.testcontainers.DockerClientFactory;

/**
 * This is the smoke test for OpenTelemetry Java agent with Oracle WebLogic. The test is ignored if
 * there are no WebLogic images installed locally. See the manual in src/weblogic directory for
 * instructions on how to build required images.
 */
class WebLogicSmokeTest extends AppServerTest {

  // FIXME: awaiting https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/1630
  private static final ExpectedServerAttributes WEBLOGIC_ATTRIBUTES =
      new ExpectedServerAttributes("", "", "");

  private static Stream<Arguments> supportedWlsConfigurations() {
    return Stream.of(
        arguments(new WebLogicConfiguration("12.2.1.4", "developer")),
        arguments(new WebLogicConfiguration("14.1.1.0", "developer-8")),
        arguments(new WebLogicConfiguration("14.1.1.0", "developer-11")));
  }

  @ParameterizedTest
  @MethodSource("supportedWlsConfigurations")
  public void webLogicSmokeTest(WebLogicConfiguration wlsConfig)
      throws IOException, InterruptedException {
    assumeTrue(
        wlsConfig.localDockerImageIsPresent(),
        "Local docker image " + wlsConfig.toString() + " is present");

    startTarget(wlsConfig.getImageName());

    // FIXME: APMI-1300
    //    assertServerHandler(....)

    assertWebAppTrace(WEBLOGIC_ATTRIBUTES);

    stopTarget();
  }

  @Override
  protected void assertMiddlewareAttributesInWebAppTrace(
      ExpectedServerAttributes serverAttributes, TraceInspector traces) {
    // FIXME: waiting for
    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/1630
  }

  static class WebLogicConfiguration {
    final String jdk;
    final String wlsVersion;

    public WebLogicConfiguration(String wlsVersion, String jdk) {
      this.jdk = jdk;
      this.wlsVersion = wlsVersion;
    }

    @Override
    public String toString() {
      return "WebLogic " + wlsVersion + " on Java " + jdk;
    }

    public String getImageName() {
      return "splunk-weblogic:" + wlsVersion + "-jdk" + jdk;
    }

    private boolean localDockerImageIsPresent() {
      try {
        DockerClientFactory.lazyClient().inspectImageCmd(getImageName()).exec();
        return true;
      } catch (Exception e) {
        System.out.println(e.getMessage());
        return false;
      }
    }
  }
}
