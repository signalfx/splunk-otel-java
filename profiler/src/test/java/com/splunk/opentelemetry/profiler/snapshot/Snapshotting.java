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

package com.splunk.opentelemetry.profiler.snapshot;

import static org.awaitility.Awaitility.await;

import com.splunk.opentelemetry.profiler.OtelLoggerFactory;
import com.splunk.opentelemetry.profiler.util.HelpfulExecutors;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.trace.IdGenerator;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

class Snapshotting {
  private static final Random RANDOM = new Random();

  static SnapshotProfilingSdkCustomizerBuilder customizer() {
    return new SnapshotProfilingSdkCustomizerBuilder();
  }

  static SnapshotProfilingAgentListener agentListener(OtelLoggerFactory otelLoggerFactory) {
    AtomicReference<SnapshotProfilingSupervisor> supervisorReference = new AtomicReference<>();
    return new SnapshotProfilingAgentListener(
        sdk -> {
          SnapshotProfilingSupervisor supervisor =
              new SnapshotProfilingSupervisor(
                  SnapshotProfilingConfiguration.SUPPLIER,
                  new LinkedBlockingQueue<>(),
                  StagingArea.SUPPLIER,
                  StackTraceSampler.SUPPLIER,
                  StackTraceExporter.SUPPLIER,
                  SpanTracker.SUPPLIER,
                  TraceThreadChangeDetector.SUPPLIER,
                  SnapshotProfilingSpanProcessor.SUPPLIER,
                  sdk,
                  otelLoggerFactory);
          supervisorReference.set(supervisor);
          supervisor.start(
              HelpfulExecutors.newSingleThreadExecutor("Test Snapshot Profiling Supervisor"));
          SnapshotProfilingSupervisor.SUPPLIER.configure(supervisor);
          return supervisor;
        }) {
      @Override
      public void afterAgent(AutoConfiguredOpenTelemetrySdk sdk) {
        super.afterAgent(sdk);
        if (SnapshotProfilingConfiguration.SUPPLIER.get().isEnabled()) {
          await().until(() -> supervisorReference.get().isRunning());
        }
      }
    };
  }

  static SnapshotProfilingAgentListener agentListener() {
    var logExporter = InMemoryLogRecordExporter.create();
    return agentListener(
        new OtelLoggerFactory(() -> logExporter, declarativeConfigProperties -> logExporter));
  }

  static void enable(SnapshotProfilingSpanProcessor... spanProcessors) {
    for (SnapshotProfilingSpanProcessor spanProcessor : spanProcessors) {
      spanProcessor.setEnabled(true);
    }
  }

  static void resetProfiling() {
    SnapshotProfilingConfiguration.SUPPLIER.reset();

    if (SnapshotProfilingSupervisor.SUPPLIER.isConfigured()) {
      SnapshotProfilingSupervisor.SUPPLIER.get().requestStopProfiling();
      await().until(() -> !SnapshotProfilingSupervisor.SUPPLIER.get().isRunning());
      SnapshotProfilingSupervisor.SUPPLIER.reset();
    }

    StackTraceSampler.SUPPLIER.reset();
    StagingArea.SUPPLIER.reset();
    StackTraceExporter.SUPPLIER.reset();

    SpanTracker.SUPPLIER.reset();
    TraceThreadChangeDetector.SUPPLIER.reset();

    SnapshotProfilingSpanProcessor.SUPPLIER.reset();
  }

  static StackTraceBuilder stackTrace() {
    var threadId = RANDOM.nextLong(10_000);
    return new StackTraceBuilder()
        .with(Instant.now())
        .with(Duration.ofMillis(20))
        .withTraceId(IdGenerator.random().generateTraceId())
        .withSpanId(IdGenerator.random().generateSpanId())
        .withId(threadId)
        .withName("thread-" + threadId)
        .with(Thread.State.WAITING)
        .with(new RuntimeException());
  }

  static SpanContextBuilder spanContext() {
    return new SpanContextBuilder();
  }

  static class SpanContextBuilder {
    private SpanContext spanContext =
        SpanContext.create(
            IdGenerator.random().generateTraceId(),
            IdGenerator.random().generateSpanId(),
            TraceFlags.getSampled(),
            TraceState.getDefault());

    SpanContextBuilder withTraceIdFrom(Span span) {
      return withTraceId(span.getSpanContext().getTraceId());
    }

    SpanContextBuilder withTraceId(String traceId) {
      spanContext =
          SpanContext.create(
              traceId,
              spanContext.getSpanId(),
              spanContext.getTraceFlags(),
              spanContext.getTraceState());
      return this;
    }

    SpanContextBuilder withSpanId(String spanId) {
      spanContext =
          SpanContext.create(
              spanContext.getTraceId(),
              spanId,
              spanContext.getTraceFlags(),
              spanContext.getTraceState());
      return this;
    }

    SpanContextBuilder unsampled() {
      spanContext =
          SpanContext.create(
              spanContext.getTraceId(),
              spanContext.getSpanId(),
              TraceFlags.getDefault(),
              spanContext.getTraceState());
      return this;
    }

    SpanContextBuilder remote() {
      spanContext =
          SpanContext.createFromRemoteParent(
              spanContext.getTraceId(),
              spanContext.getSpanId(),
              spanContext.getTraceFlags(),
              spanContext.getTraceState());
      return this;
    }

    SpanContextBuilder remoteFrom(Span span) {
      return withTraceId(span.getSpanContext().getTraceId())
          .withSpanId(span.getSpanContext().getSpanId())
          .remote();
    }

    SpanContext build() {
      return spanContext;
    }
  }

  private Snapshotting() {}
}
