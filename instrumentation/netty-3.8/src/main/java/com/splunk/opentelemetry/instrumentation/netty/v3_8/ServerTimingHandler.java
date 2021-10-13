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

package com.splunk.opentelemetry.instrumentation.netty.v3_8;

import com.splunk.opentelemetry.instrumentation.servertiming.ServerTimingHeader;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.field.VirtualField;
import io.opentelemetry.javaagent.instrumentation.netty.v3_8.NettyRequestContexts;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;

public class ServerTimingHandler extends SimpleChannelDownstreamHandler {
  private final VirtualField<Channel, NettyRequestContexts> requestContextField;

  public ServerTimingHandler(VirtualField<Channel, NettyRequestContexts> requestContextField) {
    this.requestContextField = requestContextField;
  }

  @Override
  public void writeRequested(ChannelHandlerContext ctx, MessageEvent msg) {
    NettyRequestContexts requestContexts = requestContextField.get(ctx.getChannel());
    if (requestContexts == null) {
      return;
    }

    Context context = requestContexts.context();
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
      carrier.set(key, value);
    }
  }
}
