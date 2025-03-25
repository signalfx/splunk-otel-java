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

import com.splunk.opentelemetry.profiler.snapshot.SnapshotVolumePropagatorTest.ToggleableSnapshotSelector.State;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkExtension;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SnapshotVolumePropagatorTest {
  @RegisterExtension
  public final OpenTelemetrySdkExtension sdk = OpenTelemetrySdkExtension.configure().build();

  @RegisterExtension public final ObservableCarrier carrier = new ObservableCarrier();

  private final ToggleableSnapshotSelector selector = new ToggleableSnapshotSelector();
  private final SnapshotVolumePropagator propagator = new SnapshotVolumePropagator(selector);

  @Test
  void propagatorDoesNotReportAnyFields() {
    assertEquals(Collections.emptyList(), propagator.fields());
  }

  @Test
  void attachVolumeToBaggageWhenBeginningOfTraceDetected() {
    var context = Context.current();

    var contextFromPropagator = propagator.extract(context, carrier, carrier);

    var volume = Volume.from(contextFromPropagator);
    assertEquals(Volume.HIGHEST, volume);
  }

  @Test
  void doNotAttachVolumeToBaggageWhenTraceHasAlreadyStarted(Tracer tracer) {
    var span = tracer.spanBuilder("span").startSpan();
    var context = Context.current().with(span);

    var contextFromPropagator = propagator.extract(context, carrier, carrier);

    var baggage = Baggage.fromContext(contextFromPropagator);
    assertEquals(Baggage.empty(), baggage);
  }

  @Test
  void doNotAttachVolumeToBaggageWhenTraceIsNotSelectedForSnapshotting() {
    selector.toggle(State.OFF);
    var context = Context.current();

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

  static class ToggleableSnapshotSelector implements SnapshotSelector {
    enum State {
      ON,
      OFF
    }

    private State state = State.ON;

    @Override
    public boolean select() {
      return state == State.ON;
    }

    void toggle(State state) {
      this.state = state;
    }
  }
}
