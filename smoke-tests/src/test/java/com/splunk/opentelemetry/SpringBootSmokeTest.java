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

import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SpringBootSmokeTest extends SmokeTest {

  private static Stream<Arguments> springBootConfigurations() {
    return Stream.of(
        arguments(
            new SpringBootConfiguration(8),
            new SpringBootConfiguration(11),
            new SpringBootConfiguration(14),
            new SpringBootConfiguration(15)));
  }

  @Override
  protected Map<String, String> getExtraEnv() {
    return Collections.singletonMap("SPLUNK_OTEL_CONFIG_SPAN_PROCESSOR_INSTRLIB_ENABLED", "true");
  }

  @ParameterizedTest
  @MethodSource("springBootConfigurations")
  public void springBootSmokeTestOnJDK(SpringBootConfiguration testConfig)
      throws IOException, InterruptedException {
    startTarget(testConfig::getTargetImage);

    String url = String.format("http://localhost:%d/greeting", target.getMappedPort(8080));
    Request request = new Request.Builder().url(url).get().build();

    String currentAgentVersion = getCurrentAgentVersion();

    Response response = client.newCall(request).execute();
    Collection<ExportTraceServiceRequest> traces = waitForTraces();

    Assertions.assertEquals(response.body().string(), "Hi!");
    Assertions.assertEquals(1, countSpansByName(traces, "/greeting"));
    Assertions.assertEquals(1, countSpansByName(traces, "webcontroller.greeting"));
    Assertions.assertEquals(1, countSpansByName(traces, "webcontroller.withspan"));
    Assertions.assertEquals(
        3, countFilteredAttributes(traces, "otel.library.version", currentAgentVersion));
    Assertions.assertEquals(
        3,
        countFilteredAttributes(
            traces, "splunk.instrumentation_library.version", currentAgentVersion));

    stopTarget();
  }

  static class SpringBootConfiguration {
    final int jdk;

    SpringBootConfiguration(int jdk) {
      this.jdk = jdk;
    }

    @Override
    public String toString() {
      return "Spring Boot on JDK " + jdk;
    }

    public String getTargetImage() {
      return "open-telemetry-docker-dev.bintray.io/java/smoke-springboot-jdk" + jdk + ":latest";
    }
  }
}
