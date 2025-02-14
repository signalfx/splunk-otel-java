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
import io.opentelemetry.context.propagation.TextMapGetter;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server extends Thread implements BeforeEachCallback, AfterEachCallback {
  private static final Logger logger = Logger.getLogger(Server.class.getName());

  public static Builder builder(OpenTelemetry otel) {
    return new Builder(otel);
  }

  private final BlockingQueue<Message> requests = new LinkedBlockingQueue<>();
  private final BlockingQueue<Message> responses = new LinkedBlockingQueue<>();

  private final OpenTelemetry otel;
  private final UnaryOperator<Message> operation;
  private boolean shutdown = false;


  private Server(Builder builder) {
    super(builder.name);
    this.otel = builder.otel;
    this.operation = builder.operation;
  }

  public void send(Message message) {
    requests.add(message);
  }

  public Message waitForResponse() {
    try {
      return responses.take();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void run() {
    while (!shutdown && !Thread.currentThread().isInterrupted()) {
      try {
        accept(requests.take());
      } catch (InterruptedException e) {
        logger.log(Level.WARNING, e.getMessage(), e);
      }
    }
  }

  private void accept(Message message) {
    var context =
        otel.getPropagators()
            .getTextMapPropagator()
            .extract(Context.current(), message, new MessageGetter());

    var tracer = otel.getTracer(Server.class.getName());
    var span =
        tracer.spanBuilder("process").setSpanKind(SpanKind.SERVER).setParent(context).startSpan();
    try (Scope ignored = span.makeCurrent()) {
      responses.add(operation.apply(message));
    } finally {
      span.end();
    }
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) {
    start();
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) throws Exception {
    shutdown = true;
    join(1_000);
  }

  private static class MessageGetter implements TextMapGetter<Message> {
    @Override
    public Iterable<String> keys(Message message) {
      return message.keySet();
    }

    @Override
    public String get(Message message, String key) {
      return message.get(key);
    }
  }

  public static class Builder {
    private final OpenTelemetry otel;
    private String name;
    private UnaryOperator<Message> operation = message -> message;

    private Builder(OpenTelemetry otel) {
      this.otel = otel;
    }

    public Builder named(String name) {
      this.name = name;
      return this;
    }

    public Builder performing(ExitCall.Builder builder) {
      return performing(builder.with(otel).build());
    }

    public Builder performing(UnaryOperator<Message> operation) {
      this.operation = operation;
      return this;
    }

    public Server build() {
      return new Server(this);
    }
  }
}
