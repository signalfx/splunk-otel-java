/*
 * 2024 Copyright (C) AppDynamics, Inc., and its affiliates
 * All Rights Reserved
 */

/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
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
