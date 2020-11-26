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

import com.google.protobuf.ByteString;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AppServerTest extends SmokeTest {

  private static final Logger log = LoggerFactory.getLogger(AppServerTest.class);

  protected void assertWebAppTrace() throws IOException, InterruptedException {
    String url =
        String.format(
            "http://localhost:%d/greeting?url=http://localhost:8080/headers",
            target.getMappedPort(8080));

    Request request = new Request.Builder().get().url(url).build();
    Response response = client.newCall(request).execute();

    TraceInspector traces = waitForTraces();

    Set<String> traceIds =
        traces
            .getSpanStream()
            .map(Span::getTraceId)
            .map(ByteString::toByteArray)
            .map(TraceId::bytesToHex)
            .collect(Collectors.toSet());

    Assertions.assertEquals(traceIds.size(), 1, "There is one trace");
    String theOneTraceId = new ArrayList<>(traceIds).get(0);

    String responseBody = response.body().string();

    Assertions.assertTrue(
        responseBody.contains(theOneTraceId),
        "trace id is present in the HTTP headers as reported by the called endpoint");

    Assertions.assertEquals(
        2,
        traces.countSpansByKind(Span.SpanKind.SPAN_KIND_SERVER),
        "Server spans in the distributed trace");

    Assertions.assertEquals(
        1, traces.countFilteredAttributes("http.url", url), "The span for the initial web request");
    Assertions.assertEquals(
        2,
        traces.countFilteredAttributes("http.url", "http://localhost:8080/headers"),
        "Client and server spans for the remote call");

    Assertions.assertEquals(
        1, traces.countSpansByName("GreetingServlet.withSpan"), "Span for the annotated method");

    Assertions.assertEquals(
        4,
        traces.countFilteredAttributes("otel.library.version", getCurrentAgentVersion()),
        "Number of spans tagged with current otel library version");
  }

  protected void assertServerHandler(ExpectedServerAttributes serverAttributes)
      throws IOException, InterruptedException {
    String url =
        String.format(
            "http://localhost:%d/this-is-definitely-not-there-but-there-should-be-a-trace-nevertheless",
            target.getMappedPort(8080));

    Request request = new Request.Builder().get().url(url).build();
    Response response = client.newCall(request).execute();
    log.debug("Response for non-existing page: {}", response.body().string());
    Assertions.assertEquals(
        404,
        response.code(),
        "404 response code is expected from the app-server for a request to a non-existing page.");
    var traces = waitForTraces();

    Assertions.assertEquals(1, traces.size(), "There is one trace from server handler");

    Assertions.assertEquals(
        1,
        traces.countSpansByName(serverAttributes.handlerSpanName),
        "Server span has expected name");

    Assertions.assertEquals(
        serverAttributes.middlewareName,
        getServerSpanAttribute(traces, "middleware.name"),
        "Middleware name tag on server span");
    Assertions.assertEquals(
        serverAttributes.middlewareVersion,
        getServerSpanAttribute(traces, "middleware.version"),
        "Middleware version tag on server span");

    resetBackend();
  }

  private String getServerSpanAttribute(TraceInspector traces, String attributeKey) {
    return traces
        .getSpanStream()
        .filter(span -> span.getKind() == Span.SpanKind.SPAN_KIND_SERVER)
        .map(Span::getAttributesList)
        .flatMap(Collection::stream)
        .filter(attr -> attributeKey.equals(attr.getKey()))
        .map(keyValue -> keyValue.getValue().getStringValue())
        .findFirst()
        .orElseThrow(
            () ->
                new NoSuchElementException(
                    "Attribute " + attributeKey + " is not found on server span"));
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
