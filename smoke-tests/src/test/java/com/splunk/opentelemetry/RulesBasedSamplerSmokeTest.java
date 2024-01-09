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

import java.io.IOException;
import java.util.Map;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

public class RulesBasedSamplerSmokeTest extends AppServerTest {
  protected Map<String, String> getExtraEnv() {
    return Map.of(
        "OTEL_TRACES_SAMPLER",
        "rules",
        "OTEL_TRACES_SAMPLER_ARG",
        "drop=/front;fallback=parentbased_always_on",
        "OTEL_INSTRUMENTATION_COMMON_EXPERIMENTAL_CONTROLLER_TELEMETRY_ENABLED",
        "true");
  }

  @Test
  void shouldIgnoreSampledUrl() throws IOException, InterruptedException {
    startTargetOrSkipTest(
        linuxImage(
            "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-spring-boot:jdk11-20210918.1248928124"));

    Request request = new Request.Builder().url(getUrl("/greeting", false)).get().build();
    Response response = client.newCall(request).execute();
    assertEquals(response.code(), 200);

    request = new Request.Builder().url(getUrl("/front", false)).get().build();
    response = client.newCall(request).execute();
    assertEquals(response.code(), 200);

    assertTraces(waitForTraces());

    stopTarget();
  }

  protected void assertTraces(TraceInspector traces) {
    assertEquals(1, traces.countSpansByName("GET /greeting"));
    assertEquals(1, traces.countSpansByName("WebController.greeting"));
    assertEquals(1, traces.countSpansByName("WebController.withSpan"));
    assertEquals(0, traces.countSpansByName("GET /front"));

    assertThat(traces.countTraceIds()).isEqualTo(1);
  }
}
