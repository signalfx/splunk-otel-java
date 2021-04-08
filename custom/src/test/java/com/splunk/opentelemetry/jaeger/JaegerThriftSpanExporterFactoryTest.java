package com.splunk.opentelemetry.jaeger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.test.utils.PortUtils;
import io.opentelemetry.sdk.autoconfigure.ConfigProperties;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JaegerThriftSpanExporterFactoryTest {
  static int port;
  static Server server;

  @BeforeAll
  static void startServer() throws Exception {
    port = PortUtils.randomOpenPort();
    server = new Server(port);
    for (var connector : server.getConnectors()) {
      connector.setHost("localhost");
    }

    var servletContext = new ServletContextHandler(null, null);
    servletContext.addServlet(TokenCapturingServlet.class, "/v1/trace");
    server.setHandler(servletContext);

    server.start();
  }

  @AfterAll
  static void stopServer() throws Exception {
    server.stop();
    server.destroy();
  }

  @Mock ConfigProperties config;

  @BeforeEach
  void setUp() {
    TokenCapturingServlet.CAPTURED_SPLUNK_ACCESS_TOKEN.set(null);
  }

  @Test
  void shouldCreateExporterWithSplunkAccessToken() {
    // given
    given(config.getString(JaegerThriftSpanExporterFactory.SPLUNK_ACCESS_TOKEN))
        .willReturn("token");
    given(config.getString(JaegerThriftSpanExporterFactory.OTEL_EXPORTER_JAEGER_ENDPOINT))
        .willReturn("http://localhost:" + port + "/v1/trace");

    // when
    SpanExporter exporter = new JaegerThriftSpanExporterFactory().createExporter(config);
    exporter.export(List.of(prepareMockSpanData())).join(10, TimeUnit.SECONDS);

    // then
    assertEquals("token", TokenCapturingServlet.CAPTURED_SPLUNK_ACCESS_TOKEN.get());
  }

  @Test
  void shouldCreateExporterWithoutSplunkAccessToken() {
    // given
    given(config.getString(JaegerThriftSpanExporterFactory.OTEL_EXPORTER_JAEGER_ENDPOINT))
        .willReturn("http://localhost:" + port + "/v1/trace");

    // when
    SpanExporter exporter = new JaegerThriftSpanExporterFactory().createExporter(config);
    exporter.export(List.of(prepareMockSpanData())).join(10, TimeUnit.SECONDS);

    // then
    assertNull(TokenCapturingServlet.CAPTURED_SPLUNK_ACCESS_TOKEN.get());
  }

  private static TestSpanData prepareMockSpanData() {
    return TestSpanData.builder()
        .setName("test")
        .setKind(SpanKind.INTERNAL)
        .setStartEpochNanos(1000)
        .setEndEpochNanos(2000)
        .setStatus(StatusData.unset())
        .setHasEnded(true)
        .build();
  }

  public static class TokenCapturingServlet extends HttpServlet {
    static final AtomicReference<String> CAPTURED_SPLUNK_ACCESS_TOKEN = new AtomicReference<>();

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
      CAPTURED_SPLUNK_ACCESS_TOKEN.set(request.getHeader(AuthTokenInterceptor.TOKEN_HEADER));
      response.setStatus(200);
    }
  }
}
