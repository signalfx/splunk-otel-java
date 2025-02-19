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

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SnapshotProfilingSignalPropagatorTest {
  @RegisterExtension
  public final OpenTelemetrySdkExtension sdk = OpenTelemetrySdkExtension.builder().build();

  @RegisterExtension public final ObservableCarrier carrier = new ObservableCarrier();

  private final SnapshotProfilingSignalPropagator propagator = new SnapshotProfilingSignalPropagator();

  @Test
  void propagatorDoesNotReportAnyFields() {
    assertEquals(Collections.emptyList(), propagator.fields());
  }

  @Test
  void attachVolumeToEntryWhenBeginningOfTraceDetected() {
    var context = Context.current();

    var contextFromPropagator = propagator.extract(context, carrier, carrier);

    var volume = Volume.from(contextFromPropagator);
    assertEquals(Volume.HIGHEST, volume);
  }

  @Test
  void doNotAttachVolumeToEntryWhenTraceHasAlreadyStarted(Tracer tracer) {
    var span = tracer.spanBuilder("span").startSpan();
    var context = Context.current().with(span);

    var contextFromPropagator = propagator.extract(context, carrier, carrier);

    var baggage = Baggage.fromContext(contextFromPropagator);
    assertEquals(Baggage.empty(), baggage);
  }

  @Test
  void leaveBaggageUnalteredWhenVolumeEntryDetected(Tracer tracer) {
    var span = tracer.spanBuilder("span").startSpan();
    var context = Context.current().with(Volume.HIGHEST).with(span);
    var baggage = Baggage.fromContext(context);

    var contextFromPropagator = propagator.extract(context, carrier, carrier);

    var baggageAfterPropagator = Baggage.fromContext(contextFromPropagator);
    assertEquals(baggage, baggageAfterPropagator);
  }
}
