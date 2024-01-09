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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.splunk.opentelemetry.helper.TestImage;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class SpringBootSmokeTest extends AppServerTest {

  private TestImage getTargetImage(int jdk) {
    return linuxImage(
        "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-spring-boot:jdk"
            + jdk
            + "-20230509.4927886820");
  }

  @Override
  protected Map<String, String> getExtraEnv() {
    return Map.of(
        "SPRING_APPLICATION_NAME",
        "smoke-test-app",
        "OTEL_INSTRUMENTATION_COMMON_EXPERIMENTAL_CONTROLLER_TELEMETRY_ENABLED",
        "true",
        "SPLUNK_METRICS_EXPERIMENTAL_ENABLED",
        "true");
  }

  @Override
  protected boolean shouldAutodetectServiceName() {
    return true;
  }

  @ParameterizedTest(name = "{index} => SpringBoot SmokeTest On JDK{0}.")
  @ValueSource(ints = {8, 11, 17})
  void springBootSmokeTestOnJDK(int jdk) throws IOException, InterruptedException {
    // given
    startTargetOrSkipTest(getTargetImage(jdk));

    Request request = new Request.Builder().url(getUrl("/greeting", false)).get().build();

    // when
    Response response = client.newCall(request).execute();

    // then
    assertEquals(response.body().string(), "Hi!");

    assertTraces(waitForTraces());
    assertMetrics(waitForMetrics());

    // cleanup
    stopTarget();
  }

  protected void assertTraces(TraceInspector traces) throws IOException {
    // verify spans are exported
    assertEquals(1, traces.countSpansByName("GET /greeting"));
    assertEquals(1, traces.countSpansByName("WebController.greeting"));
    assertEquals(1, traces.countSpansByName("WebController.withSpan"));

    // verify that correct agent version is set in the resource
    String currentAgentVersion = getOtelInstrumentationVersion();
    assertThat(traces.getInstrumentationLibraryVersions()).containsExactly(currentAgentVersion);

    // verify that correct service name is set in the resource
    assertTrue(traces.resourceExists("service.name", "smoke-test-app"));
    assertTrue(traces.resourceExists("splunk.distro.version", v -> !v.isEmpty()));
  }

  protected void assertMetrics(MetricsInspector metrics) {
    // verify that JVM metrics are exported
    assertTrue(metrics.hasMetricsNamed("jvm.class.loaded"));
    assertTrue(metrics.hasMetricsNamed("process.runtime.jvm.memory.allocated"));
    assertTrue(metrics.hasMetricsNamed("jvm.memory.used"));
    assertTrue(metrics.hasMetricsNamed("jvm.thread.count"));
  }

  @Test
  void shouldPropagateContextThroughHttp() throws IOException, InterruptedException {
    // given
    startTargetOrSkipTest(getTargetImage(11));

    Request request = new Request.Builder().url(getUrl("/front", false)).get().build();

    // when
    Response response = client.newCall(request).execute();
    Set<String> traceIds = waitForTraces().getTraceIds();

    // then
    assertThat(traceIds).hasSize(1);

    var traceId = traceIds.iterator().next();
    assertEquals(response.body().string(), traceId + ";" + traceId);

    // cleanup
    stopTarget();
  }
}
