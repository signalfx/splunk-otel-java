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
    assertEquals(List.of("splunk.trace.snapshot"), propagator.fields());
  }

  @Test
  void attachProfilingSignalToCarrierWhenTraceIsRegisteredForProfiling(Tracer tracer) {
    var span = tracer.spanBuilder("span").startSpan();
    var context = Context.current().with(span);
    registry.register(span.getSpanContext());

    propagator.inject(context, carrier, carrier);

    assertEquals("true", carrier.get("splunk.trace.snapshot"));
  }

  @Test
  void doNotAttachProfilingSignalToCarrierWhenTraceNotRegisteredForProfiling(Tracer tracer) {
    var span = tracer.spanBuilder("span").startSpan();
    var context = Context.current().with(span);

    propagator.inject(context, carrier, carrier);

    assertEquals(Collections.emptySet(), carrier.keys());
  }

  @Test
  void registerTraceForProfilingWhenProfilingSignalIsTrue(Tracer tracer) {
    carrier.set("splunk.trace.snapshot", "true");
    var span = tracer.spanBuilder("span").startSpan();
    var context = Context.current().with(span);

    propagator.extract(context, carrier, carrier);

    assertThat(registry.isRegistered(span.getSpanContext())).isTrue();
  }

  @Test
  void extractReturnsProvidedContextWhenRegisteringTraceForProfiling(Tracer tracer) {
    carrier.set("splunk.trace.snapshot", "true");
    var span = tracer.spanBuilder("span").startSpan();
    var context = Context.current().with(span);

    var fromPropagator = propagator.extract(context, carrier, carrier);

    assertEquals(context, fromPropagator);
  }

  @Test
  void doNotRegisterTraceForProfilingWhenProfilingSignalIsFalse(Tracer tracer) {
    carrier.set("appdynamics-profiling", "false");
    var span = tracer.spanBuilder("span").startSpan();
    var context = Context.current().with(span);

    propagator.extract(context, carrier, carrier);

    assertThat(registry.isRegistered(span.getSpanContext())).isFalse();
  }

  @Test
  void doNotRegisterTraceForProfilingWhenProfilingSignalIsMissing(Tracer tracer) {
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
