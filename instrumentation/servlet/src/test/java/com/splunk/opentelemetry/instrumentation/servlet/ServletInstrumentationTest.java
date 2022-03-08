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

package com.splunk.opentelemetry.instrumentation.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.splunk.opentelemetry.instrumentation.servertiming.ServerTimingHeader;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.test.utils.PortUtils;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.TimeoutException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ServletInstrumentationTest {
  @RegisterExtension
  static final AgentInstrumentationExtension instrumentation =
      AgentInstrumentationExtension.create();

  static final OkHttpClient httpClient = new OkHttpClient();

  static int port;
  static Server server;

  @BeforeAll
  static void startServer() throws Exception {
    port = PortUtils.findOpenPort();
    server = new Server(port);
    for (var connector : server.getConnectors()) {
      connector.setHost("localhost");
    }

    var servletContext = new ServletContextHandler(null, null);
    servletContext.addServlet(DefaultServlet.class, "/");
    servletContext.addServlet(TestServlet.class, "/servlet");
    servletContext.addFilter(TestFilter.class, "/filter", FilterMapping.REQUEST);
    servletContext.addFilter(PassThroughFilter.class, "/deep", FilterMapping.REQUEST);
    servletContext.addFilter(PassThroughFilter.class, "/deep", FilterMapping.REQUEST);
    servletContext.addServlet(TestServlet.class, "/deep");
    servletContext.addFilter(InternalSpanFilter.class, "/nested", FilterMapping.REQUEST);
    servletContext.addServlet(TestServlet.class, "/nested");
    server.setHandler(servletContext);

    server.start();
  }

  @AfterAll
  static void stopServer() throws Exception {
    server.stop();
    server.destroy();
  }

  @Test
  void shouldHttpServletAddServerTimingHeader() throws Exception {
    // given
    var request =
        new Request.Builder()
            .url(HttpUrl.get("http://localhost:" + port + "/servlet"))
            .get()
            .build();

    // when
    var response = httpClient.newCall(request).execute();

    // then
    assertEquals(200, response.code());
    assertEquals("result", response.body().string());

    var serverTimingHeader = response.header(ServerTimingHeader.SERVER_TIMING);
    assertHeaders(response, serverTimingHeader);
    assertServerTimingHeaderContainsTraceId(serverTimingHeader);
  }

  @Test
  void shouldHttpFilterAddServerTimingHeader() throws Exception {
    // given
    var request =
        new Request.Builder()
            .url(HttpUrl.get("http://localhost:" + port + "/filter"))
            .get()
            .build();

    // when
    var response = httpClient.newCall(request).execute();

    // then
    assertEquals(200, response.code());
    assertEquals("result", response.body().string());

    var serverTimingHeader = response.header(ServerTimingHeader.SERVER_TIMING);
    assertHeaders(response, serverTimingHeader);
    assertServerTimingHeaderContainsTraceId(serverTimingHeader);
  }

  @Test
  void shouldAddOnlyOneTraceParentHeader() throws Exception {
    // given
    var request =
        new Request.Builder().url(HttpUrl.get("http://localhost:" + port + "/deep")).get().build();

    // when
    var response = httpClient.newCall(request).execute();

    // then
    assertEquals(200, response.code());
    assertEquals("result", response.body().string());

    var serverTimingHeaders = response.headers(ServerTimingHeader.SERVER_TIMING);
    assertEquals(1, serverTimingHeaders.size());
    assertHeaders(response, serverTimingHeaders.get(0));
    assertServerTimingHeaderContainsTraceId(serverTimingHeaders.get(0));
  }

  @Test
  void shouldAddOnlyServerSpanAsTraceParentHeader() throws Exception {
    // given
    var request =
        new Request.Builder()
            .url(HttpUrl.get("http://localhost:" + port + "/nested"))
            .get()
            .build();

    // when
    var response = httpClient.newCall(request).execute();

    // then
    assertEquals(200, response.code());
    assertEquals("result", response.body().string());

    var serverTimingHeaders = response.headers(ServerTimingHeader.SERVER_TIMING);
    assertEquals(1, serverTimingHeaders.size());

    var serverTimingHeader = serverTimingHeaders.get(0);
    assertHeaders(response, serverTimingHeader);

    var traces = instrumentation.waitForTraces(1);
    assertEquals(1, traces.size());

    var spans = traces.get(0);
    assertEquals(2, spans.size());

    var serverSpan =
        spans.stream().filter(it -> it.getKind() == SpanKind.SERVER).findFirst().orElse(null);
    assertNotNull(serverSpan);

    var internalSpan =
        spans.stream().filter(it -> it.getKind() == SpanKind.INTERNAL).findFirst().orElse(null);
    assertNotNull(internalSpan);

    assertEquals(internalSpan.getParentSpanId(), serverSpan.getSpanId());
    assertTrue(serverTimingHeader.contains(serverSpan.getTraceId()));
    assertTrue(serverTimingHeader.contains(serverSpan.getSpanId()));
  }

  private static void assertHeaders(Response response, String serverTimingHeader) {
    assertNotNull(serverTimingHeader);
    assertEquals(
        ServerTimingHeader.SERVER_TIMING, response.header(ServerTimingHeader.EXPOSE_HEADERS));
  }

  private static void assertServerTimingHeaderContainsTraceId(String serverTimingHeader)
      throws InterruptedException, TimeoutException {
    var traces = instrumentation.waitForTraces(1);
    assertEquals(1, traces.size());

    var spans = traces.get(0);
    assertEquals(1, spans.size());

    var serverSpan = spans.get(0);
    assertTrue(serverTimingHeader.contains(serverSpan.getTraceId()));
  }

  public static class TestServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
      try (Writer writer = response.getWriter()) {
        writer.write("result");
        response.setStatus(200);
      }
    }
  }

  public static class TestFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException {
      var httpResponse = (HttpServletResponse) response;
      try (Writer writer = httpResponse.getWriter()) {
        writer.write("result");
        httpResponse.setStatus(200);
      }
    }

    @Override
    public void destroy() {}
  }

  public static class PassThroughFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
      chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
  }

  public static class InternalSpanFilter implements Filter {
    private final Tracer tracer = GlobalOpenTelemetry.getTracer("test-custom");

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

      Span span = tracer.spanBuilder("inner").startSpan();

      try (Scope scope = span.makeCurrent()) {
        chain.doFilter(request, response);
      } finally {
        span.end();
      }
    }

    @Override
    public void destroy() {}
  }
}
