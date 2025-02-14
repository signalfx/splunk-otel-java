package com.splunk.opentelemetry.profiler.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SnapshotProfilingSignalPropagatorTest {
  @RegisterExtension public final OpenTelemetrySdkExtension sdk = OpenTelemetrySdkExtension.builder().build();
  @RegisterExtension public final ObservableCarrier carrier = new ObservableCarrier();

  private final TraceRegistry registry = new TraceRegistry();
  private final SnapshotProfilingSignalPropagator propagator = new SnapshotProfilingSignalPropagator(registry);

  @Test
  void propagatorReportsOnlyProfilingSignalField() {
    assertEquals(List.of("splunk.trace.snapshot.volume"), propagator.fields());
  }

  @Test
  void attachTraceSnapshotVolumeToCarrierWhenTraceIsRegisteredForProfiling(Tracer tracer) {
    var span = tracer.spanBuilder("span").startSpan();
    var context = Context.current().with(span);
    registry.register(span.getSpanContext());

    propagator.inject(context, carrier, carrier);

    assertEquals(Volume.HIGHEST.toString(), carrier.get("splunk.trace.snapshot.volume"));
  }

  @Test
  void doNotAttachTraceSnapshotVolumeToCarrierWhenTraceNotRegisteredForProfiling(Tracer tracer) {
    var span = tracer.spanBuilder("span").startSpan();
    var context = Context.current().with(span);

    propagator.inject(context, carrier, carrier);

    assertEquals(Collections.emptySet(), carrier.keys());
  }

  @Test
  void registerTraceForProfilingWhenTraceSnapshotVolumeIsHighest(Tracer tracer) {
    carrier.set("splunk.trace.snapshot.volume", Volume.HIGHEST.toString());
    var span = tracer.spanBuilder("span").startSpan();
    var context = Context.current().with(span);

    propagator.extract(context, carrier, carrier);

    assertThat(registry.isRegistered(span.getSpanContext())).isTrue();
  }

  @Test
  void extractReturnsProvidedContextWhenRegisteringTraceForProfiling(Tracer tracer) {
    carrier.set("splunk.trace.snapshot.volume", Volume.HIGHEST.toString());
    var span = tracer.spanBuilder("span").startSpan();
    var context = Context.current().with(span);

    var fromPropagator = propagator.extract(context, carrier, carrier);

    assertEquals(context, fromPropagator);
  }

  @Test
  void doNotRegisterTraceForProfilingWhenTraceSnapshotVolumeIsOff(Tracer tracer) {
    carrier.set("splunk.trace.snapshot.volume", Volume.OFF.toString());
    var span = tracer.spanBuilder("span").startSpan();
    var context = Context.current().with(span);

    propagator.extract(context, carrier, carrier);

    assertThat(registry.isRegistered(span.getSpanContext())).isFalse();
  }

  @Test
  void doNotRegisterTraceForProfilingWhenTraceSnapshotVolumeIsMissing(Tracer tracer) {
    var span = tracer.spanBuilder("span").startSpan();
    var context = Context.current().with(span);

    propagator.extract(context, carrier, carrier);

    assertThat(registry.isRegistered(span.getSpanContext())).isFalse();
  }

  @Test
  void extractReturnsProvidedContextWhenNotRegisteringTraceForProfiling(Tracer tracer) {
    var span = tracer.spanBuilder("span").startSpan();
    var context = Context.current().with(span);

    var fromPropagator = propagator.extract(context, carrier, carrier);

    assertEquals(context, fromPropagator);
  }
}
