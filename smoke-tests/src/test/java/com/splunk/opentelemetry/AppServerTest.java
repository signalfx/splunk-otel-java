package com.splunk.opentelemetry;

import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.util.Collection;
import java.util.NoSuchElementException;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AppServerTest extends SmokeTest {

  private static final Logger log = LoggerFactory.getLogger(AppServerTest.class);

  protected void assertServerHandler(ExpectedServerAttributes serverAttributes) throws IOException, InterruptedException {
    String url =
        String.format(
            "http://localhost:%d/this-is-definitely-not-there-but-there-should-be-a-trace-nevertheless",
            target.getMappedPort(8080));

    Request request = new Request.Builder().get().url(url).build();
    Response response = client.newCall(request).execute();
    log.debug("Response for non-existing page: {}", response.body().string());
    Assertions.assertEquals(404, response.code(), "404 response code is expected from the app-server for a request to a non-existing page.");
    var traces = waitForTraces();

    Assertions.assertEquals(1, traces.size(), "There is one trace from server handler");

    Assertions.assertEquals(1, countSpansByName(traces, serverAttributes.handlerSpanName), "Server span has expected name");

    Assertions.assertEquals(serverAttributes.middlewareName, getServerSpanAttribute(traces, "middleware.name"), "Middleware name tag on server span has expected value");
    Assertions.assertEquals(serverAttributes.middlewareVersion, getServerSpanAttribute(traces, "middleware.version"), "Middleware version tag on server span has expected value");

    resetBackend();
  }

  private String getServerSpanAttribute(java.util.Collection<io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest> traces, String attributeKey) {
    return getSpanStream(traces)
        .filter(span -> span.getKind() == Span.SpanKind.SPAN_KIND_SERVER)
        .map(Span::getAttributesList)
        .flatMap(Collection::stream)
        .filter(attr -> attributeKey.equals(attr.getKey()))
        .map(keyValue -> keyValue.getValue().getStringValue())
        .findFirst().orElseThrow(() -> new NoSuchElementException("Attribute " + attributeKey + " is not found on server span"));
  }

  protected static class ExpectedServerAttributes {
    final String handlerSpanName;
    final String middlewareName;
    final String middlewareVersion;

    public ExpectedServerAttributes(String handlerSpanName, String middlewareName, String middlewareVersion) {
      this.handlerSpanName = handlerSpanName;
      this.middlewareName = middlewareName;
      this.middlewareVersion = middlewareVersion;
    }
  }
}
