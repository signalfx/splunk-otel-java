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
import io.opentelemetry.context.propagation.TextMapGetter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class Server extends Thread implements BeforeEachCallback, AfterEachCallback {
  private static final Logger logger = Logger.getLogger(Server.class.getName());

  public static Builder builder(OpenTelemetry otel) {
    return new Builder(otel);
  }

  private final BlockingQueue<Message> requests = new LinkedBlockingQueue<>();
  private final BlockingQueue<Message> responses = new LinkedBlockingQueue<>();

  private final ExecutorService executor;
  private final OpenTelemetry otel;
  private final UnaryOperator<Message> operation;
  private boolean shutdown = false;

  private Server(Builder builder) {
    super(builder.name);
    this.otel = builder.otel;
    this.operation = builder.operation;
    executor = Executors.newFixedThreadPool(builder.requestProcessingThreads);
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
        var message = requests.poll(10, TimeUnit.MILLISECONDS);
        if (message != null) {
          executor.submit(() -> accept(message));
        }
      } catch (InterruptedException e) {
        logger.log(Level.WARNING, e.getMessage(), e);
      }
    }
  }

  private void accept(Message message) {
    var context =
        otel.getPropagators()
            .getTextMapPropagator()
            .extract(Context.root(), message, new MessageGetter());
    var tracer = otel.getTracer(Server.class.getName());
    var span =
        tracer.spanBuilder("process").setSpanKind(SpanKind.SERVER).setParent(context).startSpan();
    context = context.with(span);
    try (Scope ignored2 = context.makeCurrent()) {
      responses.add(operation.apply(message));
    } finally {
      span.end();
    }
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

  @Override
  public void beforeEach(ExtensionContext extensionContext) {
    start();
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) throws Exception {
    shutdown = true;
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.SECONDS);
    join(1_000);
  }

  public static class Builder {
    private final OpenTelemetry otel;
    private String name;
    private UnaryOperator<Message> operation = message -> message;
    private int requestProcessingThreads = 1;

    private Builder(OpenTelemetry otel) {
      this.otel = otel;
    }

    public Builder named(String name) {
      this.name = name;
      return this;
    }

    public Builder requestProcessingThreads(int numberOfThreads) {
      this.requestProcessingThreads = numberOfThreads;
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
