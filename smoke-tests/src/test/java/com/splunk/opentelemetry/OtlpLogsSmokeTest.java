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
  @ValueSource(ints = {8, 11, 15})
  void springBootSmokeTestOnJDK(int jdk) throws IOException, InterruptedException {
    // given
    startTargetOrSkipTest(
        linuxImage(
            "ghcr.io/open-telemetry/java-test-containers:smoke-springboot-jdk"
                + jdk
                + "-20210218.577304949"));

    Request request = new Request.Builder().url(getUrl("/greeting", false)).get().build();

    // when
    Response response = client.newCall(request).execute();

    // then
    assertEquals(response.body().string(), "Hi!");

    Set<String> traceIds = waitForTraces().getTraceIds();
    assertThat(traceIds).hasSize(1);
    String traceId = traceIds.iterator().next();

    assertThat(
            waitForLogs()
                .getLogStream("io.opentelemetry.smoketest.springboot.controller.WebController"))
        .anyMatch(hasTraceId(traceId).and(hasStringBody("HTTP request received")));

    // cleanup
    stopTarget();
  }
}
