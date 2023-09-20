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

import static com.splunk.opentelemetry.HecTelemetryInspector.hasEventName;
import static com.splunk.opentelemetry.HecTelemetryInspector.hasTextFieldValue;
import static com.splunk.opentelemetry.LogsInspector.hasSpanId;
import static com.splunk.opentelemetry.LogsInspector.hasStringBody;
import static com.splunk.opentelemetry.LogsInspector.hasTraceId;
import static com.splunk.opentelemetry.helper.TestImage.linuxImage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class OtlpLogsSmokeTest extends SmokeTest {

  @Override
  protected Map<String, String> getExtraEnv() {
    return Map.of("OTEL_LOGS_EXPORTER", "otlp");
  }

  @ParameterizedTest(name = "{index} => OTLP logs exporter test on JDK{0}.")
  @ValueSource(ints = {8, 11, 17})
  void springBootSmokeTestOnJDK(int jdk) throws IOException, InterruptedException {
    // given
    startTargetOrSkipTest(
        linuxImage(
            "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-spring-boot:jdk"
                + jdk
                + "-20230509.4927886820"));

    Request request = new Request.Builder().url(getUrl("/greeting", false)).get().build();

    // when
    Response response = client.newCall(request).execute();

    // then
    assertEquals(response.body().string(), "Hi!");

    TraceInspector traces = waitForTraces();

    Set<String> traceIds = traces.getTraceIds();
    assertThat(traceIds).hasSize(1);

    Set<String> springSpanIds = traces.getSpanIdsByName("WebController.greeting");
    assertThat(springSpanIds).hasSize(1);

    String traceId = traceIds.iterator().next();
    String spanId = springSpanIds.iterator().next();

    assertThat(
            waitForLogs()
                .getLogStream("io.opentelemetry.smoketest.springboot.controller.WebController"))
        .anyMatch(
            hasTraceId(traceId).and(hasSpanId(spanId)).and(hasStringBody("HTTP request received")));

    if (isHecEnabled()) {
      assertThat(waitForHecEntries())
          .anyMatch(
              hasEventName("HTTP request received")
                  .and(hasTextFieldValue("span_id", spanId))
                  .and(hasTextFieldValue("trace_id", traceId)));
    }

    // cleanup
    stopTarget();
  }
}
