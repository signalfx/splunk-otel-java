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

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.splunk.opentelemetry.helper.TestImage;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TomcatSmokeTest extends AppServerTest {

  public static final ExpectedServerAttributes TOMCAT7_SERVER_ATTRIBUTES =
      new TomcatAttributes("7.0.109.0");
  public static final ExpectedServerAttributes TOMCAT8_SERVER_ATTRIBUTES =
      new TomcatAttributes("8.5.72.0");
  public static final ExpectedServerAttributes TOMCAT9_SERVER_ATTRIBUTES =
      new TomcatAttributes("9.0.54.0");
  public static final ExpectedServerAttributes TOMCAT10_SERVER_ATTRIBUTES =
      new TomcatAttributes("10.0.12.0");

  private static Stream<Arguments> supportedConfigurations() {
    return configurations("tomcat")
        .otelLinux("7.0.109", TOMCAT7_SERVER_ATTRIBUTES, VMS_HOTSPOT, "8")
        .otelLinux("8.5.72", TOMCAT8_SERVER_ATTRIBUTES, VMS_ALL, "8", "11")
        .otelLinux("9.0.54", TOMCAT9_SERVER_ATTRIBUTES, VMS_HOTSPOT, METRICS_ALL, "8", "11")
        .otelLinux("10.0.12", TOMCAT10_SERVER_ATTRIBUTES, VMS_HOTSPOT, "11", "17")
        .otelLinux("10.0.12", TOMCAT10_SERVER_ATTRIBUTES, VMS_OPENJ9, "11")
        .otelWindows("7.0.109", TOMCAT7_SERVER_ATTRIBUTES, VMS_ALL, "8")
        .otelWindows("8.5.72", TOMCAT8_SERVER_ATTRIBUTES, VMS_ALL, "8", "11")
        .otelWindows("9.0.54", TOMCAT9_SERVER_ATTRIBUTES, VMS_ALL, "8", "11")
        .otelWindows("10.0.12", TOMCAT10_SERVER_ATTRIBUTES, VMS_HOTSPOT, "11", "17")
        .otelWindows("10.0.12", TOMCAT10_SERVER_ATTRIBUTES, VMS_OPENJ9, "11")
        .stream();
  }

  @Override
  protected boolean shouldAutodetectServiceName() {
    return true;
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("supportedConfigurations")
  void tomcatSmokeTest(
      TestImage image,
      ExpectedServerAttributes expectedServerAttributes,
      String metricsImplementation)
      throws IOException, InterruptedException {
    startTargetOrSkipTest(image, metricsImplementation);

    assertServerHandler(expectedServerAttributes);
    assertWebAppTrace(expectedServerAttributes);
    assertMetrics(waitForMetrics(), metricsImplementation);

    stopTarget();
  }

  private void assertMetrics(MetricsInspector metrics, String metricsImplementation) {
    if ("micrometer".equals(metricsImplementation)) {
      assertMicrometerMetrics(metrics);
    } else if ("opentelemetry".equals(metricsImplementation)) {
      assertOtelMetrics(metrics);
    } else {
      throw new IllegalStateException("invalid metrics implementation " + metricsImplementation);
    }
  }

  private void assertMicrometerMetrics(MetricsInspector metrics) {
    var expectedAttrs = Map.of("executor_type", "tomcat");
    assertMetrics(metrics, expectedAttrs);
  }

  private void assertOtelMetrics(MetricsInspector metrics) {
    var expectedAttrs = Map.of("executor.type", "tomcat");
    assertMetrics(metrics, expectedAttrs);
  }

  private void assertMetrics(MetricsInspector metrics, Map<String, String> expectedAttrs) {
    assertTrue(metrics.hasGaugeWithAttributes("executor.threads", expectedAttrs));
    assertTrue(metrics.hasGaugeWithAttributes("executor.threads.active", expectedAttrs));
    assertTrue(metrics.hasGaugeWithAttributes("executor.threads.idle", expectedAttrs));
    assertTrue(metrics.hasGaugeWithAttributes("executor.threads.core", expectedAttrs));
    assertTrue(metrics.hasGaugeWithAttributes("executor.threads.max", expectedAttrs));
    assertTrue(metrics.hasSumWithAttributes("executor.tasks.submitted", expectedAttrs));
    assertTrue(metrics.hasSumWithAttributes("executor.tasks.completed", expectedAttrs));
  }

  private static class TomcatAttributes extends ExpectedServerAttributes {
    public TomcatAttributes(String version) {
      // This handler span name is only received if default webapps are removed
      super("GET", "tomcat", version);
    }
  }
}
