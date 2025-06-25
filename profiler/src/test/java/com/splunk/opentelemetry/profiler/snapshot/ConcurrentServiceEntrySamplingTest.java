package com.splunk.opentelemetry.profiler.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.google.perftools.profiles.ProfileProto;
import com.splunk.opentelemetry.profiler.OtelLoggerFactory;
import com.splunk.opentelemetry.profiler.pprof.PprofUtils;
import com.splunk.opentelemetry.profiler.snapshot.simulation.ExitCall;
import com.splunk.opentelemetry.profiler.snapshot.simulation.Request;
import com.splunk.opentelemetry.profiler.snapshot.simulation.Response;
import com.splunk.opentelemetry.profiler.snapshot.simulation.Server;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkExtension;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ConcurrentServiceEntrySamplingTest {
  @RegisterExtension
  private final ContextStorageResettingSpanTrackingActivator spanTrackingActivator = new ContextStorageResettingSpanTrackingActivator();

  private final InMemoryLogRecordExporter logExporter = InMemoryLogRecordExporter.create();
  private final InMemoryStagingArea staging = new InMemoryStagingArea();
  private final StackTraceSampler sampler = newSampler(staging);

  private StackTraceSampler newSampler(StagingArea staging) {
    var stagingAreaSupplier = StagingArea.SUPPLIER;
    stagingAreaSupplier.configure(staging);
    return new ScheduledExecutorStackTraceSampler(stagingAreaSupplier, SpanTracker.SUPPLIER,
        Duration.ofMillis(20));
  }

  private final SnapshotProfilingSdkCustomizer downstreamCustomizer = Snapshotting.customizer()
      .with(sampler).with(spanTrackingActivator).build();

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
          .threads(2)
          .performing(delayOf(Duration.ofMillis(500)))
          .build();

  private final SnapshotProfilingSdkCustomizer upstreamCustomizer = Snapshotting.customizer()
      .with(sampler).with(spanTrackingActivator).build();

  @RegisterExtension
  public final OpenTelemetrySdkExtension upstreamSdk =
      OpenTelemetrySdkExtension.configure()
          .withProperty("splunk.snapshot.profiler.enabled", "true")
          .with(upstreamCustomizer)
          .with(new SnapshotVolumePropagator((c) -> true))
          .build();

  @RegisterExtension
  public final Server upstream =
      Server.builder(upstreamSdk).named("upstream")
          .performing(concurrentExitCallsTo(2, downstream, upstreamSdk)).build();

  /**
   * The test is attempting to model the scenario where an upstream service makes two concurrent
   * requests into the same downstream service within the same trace. Real scenarios may differ,
   * but the important detail is that a Service receives multiple requests within the same trace
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
    upstream.send(Request.newRequest());

    await().atMost(Duration.ofSeconds(2)).until(() -> upstream.waitForResponse() != null);

    var profiledSpans = staging.allStackTraces().stream()
        .filter(s -> SpanId.isValid(s.getSpanId()))
        .map(s -> s.getTraceId() + ":" + s.getSpanId())
        .collect(Collectors.toSet());
    System.out.println(profiledSpans);

    // Downstream service should receive 2 requests within the same trace id so expect 3 total span ids (1 upstream, 2 downstream).
    // Note: A total of 5 spans will be created but the CLIENT spans in the upstream service won't
    // be profiled because they are happening in parallel in background threads which aren't yet
    // sampled
    assertThat(profiledSpans).size().isEqualTo(3);
  }

  private Set<String> findLabelValues(ProfileProto.Profile profile, AttributeKey<String> key) {
    return profile.getSampleList().stream()
        .flatMap(sampleLabels(profile))
        .filter(label(key))
        .map(Map.Entry::getValue)
        .map(String::valueOf)
        .collect(Collectors.toSet());
  }

  private Function<ProfileProto.Sample, Stream<Map.Entry<String, Object>>> sampleLabels(
      ProfileProto.Profile profile) {
    return sample -> PprofUtils.toLabelString(sample, profile).entrySet().stream();
  }

  private Predicate<Map.Entry<String, Object>> label(AttributeKey<String> key) {
    return kv -> key.getKey().equals(kv.getKey());
  }

  private Function<Request, Response> delayOf(Duration duration) {
    return request -> {
      try {
        Thread.sleep(duration.toMillis());
        return Response.from(request);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    };
  }

  private Function<Request, Response> concurrentExitCallsTo(int calls, Server server,
      OpenTelemetry otel) {
    return request -> {
      var executor = Context.current().wrap(Executors.newFixedThreadPool(calls));
      var futures = new ArrayList<Future<Response>>();
      try {
        for (int i = 0; i < calls; ++i) {
          futures.add(executor.submit(() -> ExitCall.to(server).with(otel).build().apply(request)));
        }

        for (var future : futures) {
          try {
            future.get();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }

        return Response.from(request);
      } finally {
        executor.shutdown();
      }
    };
  }
}
