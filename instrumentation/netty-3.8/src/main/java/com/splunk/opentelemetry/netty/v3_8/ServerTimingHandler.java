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

import static io.opentelemetry.javaagent.instrumentation.netty.v3_8.server.NettyHttpServerTracer.tracer;

import com.splunk.opentelemetry.servertiming.ServerTimingHeader;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.javaagent.instrumentation.api.ContextStore;
import io.opentelemetry.javaagent.instrumentation.netty.v3_8.ChannelTraceContext;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;

public class ServerTimingHandler extends SimpleChannelDownstreamHandler {
  private final ContextStore<Channel, ChannelTraceContext> contextStore;

  public ServerTimingHandler(ContextStore<Channel, ChannelTraceContext> contextStore) {
    this.contextStore = contextStore;
  }

  @Override
  public void writeRequested(ChannelHandlerContext ctx, MessageEvent msg) {
    ChannelTraceContext channelTraceContext =
        contextStore.putIfAbsent(ctx.getChannel(), ChannelTraceContext.Factory.INSTANCE);

    Context context = tracer().getServerContext(channelTraceContext);
    if (context == null || !(msg.getMessage() instanceof HttpResponse)) {
      ctx.sendDownstream(msg);
      return;
    }

    HttpResponse response = (HttpResponse) msg.getMessage();
    ServerTimingHeader.setHeaders(context, response.headers(), HeadersSetter.INSTANCE);
    ctx.sendDownstream(msg);
  }

  public static final class HeadersSetter implements TextMapSetter<HttpHeaders> {
    private static final HeadersSetter INSTANCE = new HeadersSetter();

    @Override
    public void set(HttpHeaders carrier, String key, String value) {
      carrier.add(key, value);
    }
  }
}
