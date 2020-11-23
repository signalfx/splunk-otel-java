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

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import java.io.IOException;
import java.util.Collection;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SpringBootSmokeTest extends SmokeTest {

  private String getTargetImage(int jdk) {
    return "open-telemetry-docker-dev.bintray.io/java/smoke-springboot-jdk"
        + jdk
        + ":20201105.347264626";
  }

  @ParameterizedTest(name = "{index} => SpringBoot SmokeTest On JDK{0}.")
  @ValueSource(ints = {8, 11, 15})
  public void springBootSmokeTestOnJDK(int jdk) throws IOException, InterruptedException {
    startTarget(getTargetImage(jdk));

    String url = String.format("http://localhost:%d/greeting", target.getMappedPort(8080));
    Request request = new Request.Builder().url(url).get().build();

    String currentAgentVersion = getCurrentAgentVersion();

    Response response = client.newCall(request).execute();
    Collection<ExportTraceServiceRequest> traces = waitForTraces();

    Assertions.assertEquals(response.body().string(), "Hi!");
    Assertions.assertEquals(1, countSpansByName(traces, "/greeting"));
    Assertions.assertEquals(1, countSpansByName(traces, "WebController.greeting"));
    Assertions.assertEquals(1, countSpansByName(traces, "WebController.withSpan"));
    Assertions.assertEquals(
        3, countFilteredAttributes(traces, "otel.library.version", currentAgentVersion));

    stopTarget();
  }
}
