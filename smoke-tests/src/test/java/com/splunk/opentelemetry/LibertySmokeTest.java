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

import com.splunk.opentelemetry.helper.ResourceMapping;
import com.splunk.opentelemetry.helper.TestImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;
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
        .otelWindows("20.0.0.12", LIBERTY20_SERVER_ATTRIBUTES, VMS_ALL, "8", "11", "15")
        .stream();
  }

  protected List<ResourceMapping> getExtraResources() {
    return List.of(
        // server.xml path on linux containers
        ResourceMapping.of("liberty-with-monitor.xml", "/config/server.xml"),
        // server.xml path on windows containers
        ResourceMapping.of(
            "liberty-with-monitor.xml", "/server/usr/servers/defaultServer/server.xml"));
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("supportedConfigurations")
  void libertySmokeTest(TestImage image, ExpectedServerAttributes expectedServerAttributes)
      throws IOException, InterruptedException {
    startTargetOrSkipTest(image);

    assertServerHandler(expectedServerAttributes);
    assertWebAppTrace(expectedServerAttributes);

    // TODO: Windows collector image cannot accept signalfx metrics right now, don't assert them
    if (!image.isWindows()) {
      assertMetrics(waitForMetrics());
    }

    stopTarget();
  }

  private void assertMetrics(MetricsInspector metrics) {
    var expectedAttrs = Map.of("executor_type", "liberty");
    assertTrue(metrics.hasGaugeWithAttributes("executor.threads", expectedAttrs));
    assertTrue(metrics.hasGaugeWithAttributes("executor.threads.active", expectedAttrs));
  }

  public static class LibertyAttributes extends ExpectedServerAttributes {
    public LibertyAttributes(String version) {
      super("HTTP GET", "websphere liberty", version);
    }
  }
}
