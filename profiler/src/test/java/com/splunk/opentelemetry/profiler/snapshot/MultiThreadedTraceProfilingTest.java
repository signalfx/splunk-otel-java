package com.splunk.opentelemetry.profiler.snapshot;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.splunk.opentelemetry.profiler.snapshot.simulation.Message;
import com.splunk.opentelemetry.profiler.snapshot.simulation.Server;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkExtension;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MultiThreadedTraceProfilingTest {
  private final InMemoryStagingArea staging = new InMemoryStagingArea();
  private final SnapshotProfilingSdkCustomizer customizer = Snapshotting.customizer()
      .withRealStackTraceSampler()
      .with(staging)
      .build();

  @RegisterExtension
  public final OpenTelemetrySdkExtension sdk =
      OpenTelemetrySdkExtension.configure()
          .withProperty("splunk.snapshot.profiler.enabled", "true")
          .with(customizer)
          .with(new SnapshotVolumePropagator((c) -> true))
          .build();

  @RegisterExtension
  public final Server server =
      Server.builder(sdk).named("server").performing(message -> {
        var executor = Context.current().wrap(Executors.newSingleThreadExecutor());
        try {
          var future = executor.submit(() -> {
            var span = sdk.getTracer("server-bg-thread").spanBuilder("server-background").startSpan();
            try (var ignored = span.makeCurrent()) {
              Thread.sleep(250);
              return UUID.randomUUID();
            }
          });

          try {
            future.get();
            return message;
          } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
          }
        } finally {
          executor.shutdown();
        }
      }).build();

  @Test
  void traceIsProfiledAcrossMultipleThreads() {
    server.send(new Message());

    await().atMost(Duration.ofSeconds(2)).until(() -> server.waitForResponse() != null);

    var profiledThreads =
        staging.allStackTraces().stream()
            .filter(s -> SpanId.isValid(s.getSpanId()))
            .map(StackTrace::getThreadId)
            .collect(Collectors.toSet());

    // Server delegates some of its work to a single background thread. The background
    // thread should be included in the same trace context and be profiled.
    assertEquals(2, profiledThreads.size());
  }
}

