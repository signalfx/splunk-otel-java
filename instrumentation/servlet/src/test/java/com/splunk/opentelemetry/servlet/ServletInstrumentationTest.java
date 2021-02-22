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

package com.splunk.opentelemetry.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.splunk.opentelemetry.servertiming.ServerTimingHeader;
import io.opentelemetry.instrumentation.test.utils.OkHttpUtils;
import io.opentelemetry.instrumentation.test.utils.PortUtils;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.TimeoutException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
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

  static final OkHttpClient httpClient = OkHttpUtils.client();

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
    servletContext.addServlet(DefaultServlet.class, "/");
    servletContext.addServlet(TestServlet.class, "/servlet");
    servletContext.addFilter(TestFilter.class, "/filter", FilterMapping.REQUEST);
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

  private static void assertHeaders(Response response, String serverTimingHeader) {
    assertNotNull(serverTimingHeader);
    assertEquals(
        ServerTimingHeader.SERVER_TIMING, response.header(ServerTimingHeader.EXPOSE_HEADERS));
  }

  private static void assertServerTimingHeaderContainsTraceId(String serverTimingHeader)
      throws InterruptedException, TimeoutException {
    instrumentation.waitForTraces(1);

    var serverSpan = instrumentation.spans().get(0);
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
}
