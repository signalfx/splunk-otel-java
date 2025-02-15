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

package com.splunk.opentelemetry.profiler.snapshot.simulation;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.util.function.UnaryOperator;

public class ExitCall implements UnaryOperator<Message> {
  public static Builder to(Server server) {
    return new Builder(server);
  }

  private final OpenTelemetry otel;
  private final Server server;

  private ExitCall(Builder builder) {
    this.otel = builder.otel;
    this.server = builder.server;
  }

  @Override
  public Message apply(Message input) {
    var tracer = otel.getTracer(ExitCall.class.getName());
    var span =
        tracer
            .spanBuilder("send")
            .setSpanKind(SpanKind.CLIENT)
            .setParent(Context.current())
            .startSpan();

    try (Scope ignored = span.makeCurrent()) {
      otel.getPropagators().getTextMapPropagator().inject(Context.current(), input, Message::put);
      server.send(input);
      return server.waitForResponse();
    } finally {
      span.end();
    }
  }

  public static class Builder {
    private final Server server;
    private OpenTelemetry otel;

    private Builder(Server server) {
      this.server = server;
    }

    public Builder with(OpenTelemetry otel) {
      this.otel = otel;
      return this;
    }

    public ExitCall build() {
      return new ExitCall(this);
    }
  }
}
