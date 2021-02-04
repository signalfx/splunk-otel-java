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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AppServerTest extends SmokeTest {

  private static final Logger log = LoggerFactory.getLogger(AppServerTest.class);

  /**
   * The test case is expected to create and verify the following trace: <code>
   *   1. Server span for the initial request to http://localhost:%d/greeting?url=http://localhost:8080/headers
   *   2. Client http span to http://localhost:8080/headers
   *   3. Server http span for http://localhost:8080/headers
   * </code>
   */
  protected void assertWebAppTrace(ExpectedServerAttributes serverAttributes)
      throws IOException, InterruptedException {
    String url = getUrl("/app/greeting", false);

    Request request = new Request.Builder().get().url(url).build();
    String responseHeadersAndBody = tryGetResponse(request);

    TraceInspector traces = waitForTraces();

    assertEquals(1, traces.countTraceIds(), "There is one trace");

    Set<String> traceIds = traces.getTraceIds();
    String theOneTraceId = new ArrayList<>(traceIds).get(0);

    assertTrue(
        responseHeadersAndBody.contains(theOneTraceId),
        "trace id is present in the HTTP headers as reported by the called endpoint");

    assertEquals(
        2,
        traces.countSpansByKind(Span.SpanKind.SPAN_KIND_SERVER),
        "Server spans in the distributed trace");
    assertMiddlewareAttributesInWebAppTrace(serverAttributes, traces);

    assertEquals(
        1, traces.countFilteredAttributes("http.url", url), "The span for the initial web request");
    assertEquals(
        2,
        traces.countFilteredAttributes("http.url", getUrl("/app/headers", true)),
        "Client and server spans for the remote call");

    assertEquals(
        totalNumberOfSpansInWebappTrace(),
        traces.countFilteredAttributes("otel.library.version", getCurrentAgentVersion()),
        "Number of spans tagged with current otel library version");
  }

  /*
   * Some app servers, e.g. WebLogic 12.1.3 open their HTTP listen port before
   * an application gets deployed. Which results in all kinds of response codes
   * ranging from 404 to 503 to be returned instead of expected response.
   * This method retries for 30 seconds to get a 200 response after the container's
   * port is open and docker thinks the container is good for use.
   */
  private String tryGetResponse(Request request) throws IOException {
    long startTime = System.currentTimeMillis();
    Response response;
    String responseBody;
    do {
      response = client.newCall(request).execute();
      responseBody = response.body().string();
      System.out.println("Got response code " + response.code());
    } while (response.code() != 200
        && System.currentTimeMillis() - startTime < TimeUnit.SECONDS.toMillis(30));

    assertEquals(
        200, response.code(), "Unexpected response code. Got this response: " + responseBody);
    return responseBody;
  }

  private int totalNumberOfSpansInWebappTrace() {
    // 1) Incoming /greeting
    // 2) Outgoing /headers
    // 3) Incoming /headers
    return 3;
  }

  protected void assertMiddlewareAttributesInWebAppTrace(
      ExpectedServerAttributes serverAttributes, TraceInspector traces) {
    assertEquals(
        2,
        traces.countFilteredAttributes("middleware.name", serverAttributes.middlewareName),
        "Middleware name is present on all server spans");
    assertEquals(
        2,
        traces.countFilteredAttributes("middleware.version", serverAttributes.middlewareVersion),
        "Middleware version is present on all server spans");
  }

  protected void assertServerHandler(ExpectedServerAttributes serverAttributes)
      throws IOException, InterruptedException {
    String url =
        getUrl("/this-is-definitely-not-there-but-there-should-be-a-trace-nevertheless", false);

    Request request = new Request.Builder().get().url(url).build();
    Response response = client.newCall(request).execute();
    log.debug("Response for non-existing page: {}", response.body().string());
    assertEquals(
        404,
        response.code(),
        "404 response code is expected from the app-server for a request to a non-existing page.");
    var traces = waitForTraces();

    var numberOfTraceIds = traces.countTraceIds();
    assertEquals(1, numberOfTraceIds, "There is one trace from server handler");

    assertEquals(
        1,
        traces.countSpansByName(serverAttributes.handlerSpanName),
        "Server span has expected name");

    assertEquals(
        serverAttributes.middlewareName,
        traces.getServerSpanAttribute("middleware.name"),
        "Middleware name tag on server span");
    assertEquals(
        serverAttributes.middlewareVersion,
        traces.getServerSpanAttribute("middleware.version"),
        "Middleware version tag on server span");

    resetBackend();
  }

  protected String getUrl(String path, boolean originalPort) {
    int port = originalPort ? 8080 : containerManager.getTargetMappedPort(8080);
    return String.format("http://localhost:%d%s", port, path);
  }

  protected static class ExpectedServerAttributes {
    final String handlerSpanName;
    final String middlewareName;
    final String middlewareVersion;

    public ExpectedServerAttributes(
        String handlerSpanName, String middlewareName, String middlewareVersion) {
      this.handlerSpanName = handlerSpanName;
      this.middlewareName = middlewareName;
      this.middlewareVersion = middlewareVersion;
    }
  }
}
