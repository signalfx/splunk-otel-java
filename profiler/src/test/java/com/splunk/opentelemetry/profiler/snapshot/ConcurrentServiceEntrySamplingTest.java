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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.splunk.opentelemetry.profiler.OtelLoggerFactory;
import com.splunk.opentelemetry.profiler.snapshot.simulation.Delay;
import com.splunk.opentelemetry.profiler.snapshot.simulation.ExitCall;
import com.splunk.opentelemetry.profiler.snapshot.simulation.Message;
import com.splunk.opentelemetry.profiler.snapshot.simulation.Server;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkExtension;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ConcurrentServiceEntrySamplingTest {
  @RegisterExtension
  private final ContextStorageResettingSpanTrackingActivator spanTrackingActivator =
      new ContextStorageResettingSpanTrackingActivator();

  private final InMemoryLogRecordExporter logExporter = InMemoryLogRecordExporter.create();
  private final InMemoryStagingArea staging = new InMemoryStagingArea();
  private final StackTraceSampler sampler = newSampler(staging);

  private StackTraceSampler newSampler(StagingArea staging) {
    var stagingAreaSupplier = StagingArea.SUPPLIER;
    stagingAreaSupplier.configure(staging);
    return new PeriodicStackTraceSampler(
        stagingAreaSupplier, SpanTracker.SUPPLIER, Duration.ofMillis(20));
  }

  private final SnapshotProfilingSdkCustomizer downstreamCustomizer =
      Snapshotting.customizer().with(sampler).with(spanTrackingActivator).build();

  @RegisterExtension
  public final OpenTelemetrySdkExtension downstreamSdk =
      OpenTelemetrySdkExtension.configure()
          .withProperty("splunk.snapshot.profiler.enabled", "true")
          .with(downstreamCustomizer)
          .with(new StackTraceExporterActivator(new OtelLoggerFactory(properties -> logExporter)))
          .build();

  @RegisterExtension
  public final Server downstream =
      Server.builder(downstreamSdk)
          .named("downstream")
          .requestProcessingThreads(2)
          .performing(Delay.of(Duration.ofMillis(500)))
          .build();

  private final SnapshotProfilingSdkCustomizer upstreamCustomizer =
      Snapshotting.customizer().with(sampler).with(spanTrackingActivator).build();

  @RegisterExtension
  public final OpenTelemetrySdkExtension upstreamSdk =
      OpenTelemetrySdkExtension.configure()
          .withProperty("splunk.snapshot.profiler.enabled", "true")
          .with(upstreamCustomizer)
          .with(new SnapshotVolumePropagator((c) -> true))
          .build();

  @RegisterExtension
  public final Server upstream =
      Server.builder(upstreamSdk)
          .named("upstream")
          .performing(concurrentExitCallsTo(2, downstream, upstreamSdk))
          .build();

  /**
   * The test is attempting to model the scenario where an upstream service makes two concurrent
   * requests into the same downstream service within the same trace. Real scenarios may differ, but
   * the important detail is that a Service receives multiple requests within the same trace
   * concurrently. When this happens we want both threads in the downstream service to be sampled.
   *
   * <pre>{@code
   *                   +------------+
   *               +-> | downstream |
   * +----------+  |   +------------+
   * | upstream | -+
   * +----------+  |   +------------+
   *               +-> | downstream |
   *                   +------------+
   * }</pre>
   */
  @Test
  void profileMultipleConcurrentServiceEntries() {
    upstream.send(new Message());

    await().atMost(Duration.ofSeconds(2)).until(() -> upstream.waitForResponse() != null);

    var profiledSpans =
        staging.allStackTraces().stream()
            .filter(s -> SpanId.isValid(s.getSpanId()))
            .map(s -> s.getTraceId() + ":" + s.getSpanId())
            .collect(Collectors.toSet());

    // Downstream service should receive 2 requests within the same trace id so expect 3 total span
    // ids (1 upstream, 2 downstream).
    // Note: A total of 5 spans will be created, but the CLIENT spans in the upstream service won't
    // be profiled because they are happening in parallel on background threads which aren't yet
    // sampled
    assertThat(profiledSpans).size().isEqualTo(3);
  }

  private UnaryOperator<Message> concurrentExitCallsTo(
      int calls, Server server, OpenTelemetry otel) {
    return message -> {
      var executor = Context.current().wrap(Executors.newFixedThreadPool(calls));
      var futures = new ArrayList<Future<Message>>();
      try {
        for (int i = 0; i < calls; ++i) {
          futures.add(executor.submit(() -> ExitCall.to(server).with(otel).build().apply(message)));
        }

        for (var future : futures) {
          try {
            future.get();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }

        return message;
      } finally {
        executor.shutdown();
      }
    };
  }
}
