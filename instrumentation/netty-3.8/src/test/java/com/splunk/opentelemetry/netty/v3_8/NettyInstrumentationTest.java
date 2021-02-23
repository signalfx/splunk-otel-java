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

package com.splunk.opentelemetry.netty.v3_8;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.splunk.opentelemetry.servertiming.ServerTimingHeader;
import io.opentelemetry.instrumentation.test.utils.OkHttpUtils;
import io.opentelemetry.instrumentation.test.utils.PortUtils;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeoutException;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.DefaultChannelPipeline;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.SucceededChannelFuture;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpServerCodec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class NettyInstrumentationTest {
  @RegisterExtension
  static final AgentInstrumentationExtension instrumentation =
      AgentInstrumentationExtension.create();

  static final OkHttpClient httpClient = OkHttpUtils.client();

  static int port;
  static ServerBootstrap server;

  @BeforeAll
  static void startServer() {
    port = PortUtils.randomOpenPort();

    server = new ServerBootstrap(new NioServerSocketChannelFactory());
    server.setPipelineFactory(
        () -> {
          var pipeline = new DefaultChannelPipeline();
          pipeline.addLast("http-codec", new HttpServerCodec());
          pipeline.addLast(
              "controller",
              new SimpleChannelHandler() {
                @Override
                public void messageReceived(ChannelHandlerContext ctx, MessageEvent msg) {
                  if (msg.getMessage() instanceof HttpRequest) {
                    var responseBody = ChannelBuffers.copiedBuffer("result", UTF_8);
                    var response = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK);
                    response.setContent(responseBody);
                    response.headers().set(CONTENT_TYPE, "text/plain");
                    response.headers().set(CONTENT_LENGTH, responseBody.readableBytes());

                    ctx.sendDownstream(
                        new DownstreamMessageEvent(
                            ctx.getChannel(),
                            new SucceededChannelFuture(ctx.getChannel()),
                            response,
                            ctx.getChannel().getRemoteAddress()));
                  }
                }
              });
          return pipeline;
        });

    server.bind(new InetSocketAddress(port));
  }

  @AfterAll
  static void stopServer() {
    server.shutdown();
  }

  @Test
  void shouldAddServerTimingHeaders() throws Exception {
    // given
    var request = new Request.Builder().url(HttpUrl.get("http://localhost:" + port)).get().build();

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
    var traces = instrumentation.waitForTraces(1);
    assertEquals(1, traces.size());

    var spans = traces.get(0);
    assertEquals(1, spans.size());

    var serverSpan = spans.get(0);
    assertTrue(serverTimingHeader.contains(serverSpan.getTraceId()));
  }
}
