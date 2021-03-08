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

import static io.opentelemetry.javaagent.instrumentation.netty.v4_0.server.NettyHttpServerTracer.tracer;

import com.splunk.opentelemetry.servertiming.ServerTimingHeader;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;

public class ServerTimingHandler extends ChannelOutboundHandlerAdapter {
  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise prm) {
    Context context = tracer().getServerContext(ctx.channel());
    if (context == null || !(msg instanceof HttpResponse)) {
      ctx.write(msg, prm);
      return;
    }

    HttpResponse response = (HttpResponse) msg;
    ServerTimingHeader.setHeaders(context, response.headers(), HeadersSetter.INSTANCE);
    ctx.write(msg, prm);
  }

  public static final class HeadersSetter implements TextMapSetter<HttpHeaders> {
    private static final HeadersSetter INSTANCE = new HeadersSetter();

    @Override
    public void set(HttpHeaders carrier, String key, String value) {
      carrier.add(key, value);
    }
  }
}
