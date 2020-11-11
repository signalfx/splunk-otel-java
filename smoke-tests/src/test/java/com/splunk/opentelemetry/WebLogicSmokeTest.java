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

import com.google.protobuf.ByteString;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.DockerClientFactory;

/**
 * This is the smoke test for OpenTelemetry Java agent with Oracle WebLogic. The test is ignored if
 * there are no WebLogic images installed locally. See the manual in src/weblogic directory for
 * instructions on how to build required images.
 */
class WebLogicSmokeTest extends SmokeTest {

  private static Stream<Arguments> supportedWlsConfigurations() {
    return Stream.of(
        arguments(new WebLogicConfiguration(12, 8)),
        arguments(new WebLogicConfiguration(14, 8)),
        arguments(new WebLogicConfiguration(14, 11)));
  }

  @ParameterizedTest
  @MethodSource("supportedWlsConfigurations")
  public void webLogicSmokeTest(WebLogicConfiguration wlsConfig)
      throws IOException, InterruptedException {
    assumeTrue(
        wlsConfig.localDockerImageIsPresent(),
        "Local docker image " + wlsConfig.toString() + " is present");

    startTarget(wlsConfig.getImageName());

    String url =
        String.format(
            "http://localhost:%d/wls-demo/greetingRemote?url=http://localhost:8080/wls-demo/headers",
            target.getMappedPort(8080));

    Request request = new Request.Builder().get().url(url).build();

    String currentAgentVersion = getCurrentAgentVersion();

    Response response = client.newCall(request).execute();
    Collection<ExportTraceServiceRequest> traces = waitForTraces();

    Set<ByteString> traceIds =
        getSpanStream(traces).map(Span::getTraceId).collect(Collectors.toSet());

    Assertions.assertEquals(traceIds.size(), 1, "There is one trace");

    String theOneTraceId =
        traceIds.stream()
            .findFirst()
            .map(ByteString::toByteArray)
            .map(TraceId::bytesToHex)
            .orElseThrow(AssertionError::new);

    String responseBody = response.body().string();
    Assertions.assertTrue(
        responseBody.contains("bytes read by weblogic.net.http.SOAPHttpURLConnection"),
        "HTTP Call is made using weblogic own url connection");
    Assertions.assertTrue(
        responseBody.contains(theOneTraceId),
        "trace id is present in the HTTP headers as reported by the called endpoint");

    Assertions.assertEquals(
        1,
        countSpansByName(traces, "/wls-demo/greetingRemote"),
        "The span for the initial web request");
    Assertions.assertEquals(
        1,
        countSpansByName(traces, "/wls-demo/headers"),
        "The span for the web request called from the controller");
    Assertions.assertEquals(
        1,
        countSpansByName(traces, "TheController.showRequestHeaders"),
        "The span for the web framework controller");
    Assertions.assertEquals(
        1,
        countSpansByName(traces, "TheController.sayRemoteHello"),
        "The span for the web framework controller");
    Assertions.assertEquals(
        1, countSpansByName(traces, "TheController.withSpan"), "Spans for the annotated methods");
    Assertions.assertEquals(
        6,
        countFilteredAttributes(traces, "otel.library.version", currentAgentVersion),
        "Number of spans tagged with current otel library version");

    stopTarget();
  }

  static class WebLogicConfiguration {
    final int jdk;
    final int wlsMajorVersion;

    public WebLogicConfiguration(int wlsMajorVersion, int jdk) {
      this.jdk = jdk;
      this.wlsMajorVersion = wlsMajorVersion;
    }

    @Override
    public String toString() {
      return "WebLogic " + wlsMajorVersion + " on Java " + jdk;
    }

    public String getImageName() {
      return "open-telemetry-docker-dev.bintray.io/java/smoke-weblogic"
          + wlsMajorVersion
          + "-jdk"
          + jdk
          + "-demo:latest";
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
