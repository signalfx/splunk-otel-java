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

package com.splunk.opentelemetry.netty.v4_0;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.splunk.opentelemetry.servertiming.ServerTimingHeader;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpServerCodec;
import io.opentelemetry.instrumentation.test.utils.OkHttpUtils;
import io.opentelemetry.instrumentation.test.utils.PortUtils;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import java.util.concurrent.TimeoutException;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class NettyInstrumentationTest {
  @RegisterExtension
  static final AgentInstrumentationExtension instrumentation =
      AgentInstrumentationExtension.create();

  static final OkHttpClient httpClient = OkHttpUtils.client();

  static int port;
  static EventLoopGroup server;

  @BeforeAll
  static void startServer() throws InterruptedException {
    port = PortUtils.randomOpenPort();
    server = new NioEventLoopGroup();

    new ServerBootstrap()
        .group(server)
        .childHandler(
            new ChannelInitializer<>() {
              @Override
              protected void initChannel(@NotNull Channel ch) {
                var pipeline = ch.pipeline();
                pipeline.addLast(new HttpServerCodec());
                pipeline.addLast(
                    new SimpleChannelInboundHandler<HttpRequest>() {
                      @Override
                      protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) {
                        var responseBody = Unpooled.copiedBuffer("result", UTF_8);
                        var response = new DefaultFullHttpResponse(HTTP_1_1, OK, responseBody);
                        response.headers().set(CONTENT_TYPE, "text/plain");
                        response.headers().set(CONTENT_LENGTH, responseBody.readableBytes());
                        ctx.write(response);
                        ctx.flush();
                      }
                    });
              }
            })
        .channel(NioServerSocketChannel.class)
        .bind(port)
        .sync();
  }

  @AfterAll
  static void stopServer() {
    server.shutdownGracefully();
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
