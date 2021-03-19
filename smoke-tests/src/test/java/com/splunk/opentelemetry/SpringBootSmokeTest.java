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

import static com.splunk.opentelemetry.helper.TestImage.linuxImage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.splunk.opentelemetry.helper.TestImage;
import java.io.IOException;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SpringBootSmokeTest extends AppServerTest {

  private TestImage getTargetImage(int jdk) {
    return linuxImage(
        "open-telemetry-docker-dev.bintray.io/java/smoke-springboot-jdk"
            + jdk
            + ":20201105.347264626");
  }

  @ParameterizedTest(name = "{index} => SpringBoot SmokeTest On JDK{0}.")
  @ValueSource(ints = {8, 11, 15})
  public void springBootSmokeTestOnJDK(int jdk) throws IOException, InterruptedException {
    startTargetOrSkipTest(getTargetImage(jdk));

    Request request = new Request.Builder().url(getUrl("/greeting", false)).get().build();

    Response response = client.newCall(request).execute();

    TraceInspector traces = waitForTraces();
    MetricsInspector metrics = waitForMetrics();

    // verify spans are exported
    assertEquals(response.body().string(), "Hi!");
    assertEquals(1, traces.countSpansByName("/greeting"));
    assertEquals(1, traces.countSpansByName("WebController.greeting"));

    // verify that correct agent version is set in the resource
    String currentAgentVersion = getCurrentAgentVersion();
    assertEquals(2, traces.countFilteredAttributes("otel.library.version", currentAgentVersion));

    // verify that correct service name is set in the resource
    assertTrue(traces.resourceExists("service.name", "smoke-test"));

    // verify that JVM metrics are exported
    assertTrue(metrics.hasMetricsNamed("runtime.jvm.classes.loaded"));
    assertTrue(metrics.hasMetricsNamed("runtime.jvm.gc.memory.allocated"));
    assertTrue(metrics.hasMetricsNamed("runtime.jvm.memory.used"));
    assertTrue(metrics.hasMetricsNamed("runtime.jvm.threads.peak"));

    stopTarget();
  }
}
