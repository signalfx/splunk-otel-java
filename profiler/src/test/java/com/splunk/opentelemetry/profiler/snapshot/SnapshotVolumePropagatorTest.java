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
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class SnapshotVolumePropagatorTest {
  @RegisterExtension public final ObservableCarrier carrier = new ObservableCarrier();

  private final ToggleableSnapshotSelector selector = new ToggleableSnapshotSelector();
  private final SnapshotVolumePropagator propagator = new SnapshotVolumePropagator(selector);

  @Test
  void propagatorDoesNotReportAnyFields() {
    assertEquals(Collections.emptyList(), propagator.fields());
  }

  @Test
  void selectTraceForSnapshottingByAttachingHighestVolumeToBaggage() {
    selector.toggle(State.ON);

    var contextFromPropagator = propagator.extract(Context.root(), carrier, carrier);

    var volume = Volume.from(contextFromPropagator);
    assertEquals(Volume.HIGHEST, volume);
  }

  @Test
  void rejectTraceForSnapshottingByAttachingOffVolumeToBaggage() {
    selector.toggle(State.OFF);

    var contextFromPropagator = propagator.extract(Context.root(), carrier, carrier);

    var volume = Volume.from(contextFromPropagator);
    assertEquals(Volume.OFF, volume);
  }

  @ParameterizedTest
  @EnumSource(value = Volume.class, mode = Mode.EXCLUDE, names = "UNSPECIFIED")
  void retainTraceSelectionDecisionFromUpstreamServices(Volume volume) {
    var spanContext = Snapshotting.spanContext().remote().build();
    var parentSpan = Span.wrap(spanContext);
    var context = Context.root().with(parentSpan).with(volume);

    var contextFromPropagator = propagator.extract(context, carrier, carrier);

    var volumeFromContext = Volume.from(contextFromPropagator);
    assertEquals(volume, volumeFromContext);
  }

  @Test
  void selectTraceForSnapshottingDecisionLeftUndecidedAndTraceIsSelected() {
    selector.toggle(State.ON);

    var spanContext = Snapshotting.spanContext().remote().build();
    var parentSpan = Span.wrap(spanContext);
    var context = Context.root().with(parentSpan);

    var contextFromPropagator = propagator.extract(context, carrier, carrier);

    var volumeFromContext = Volume.from(contextFromPropagator);
    assertEquals(Volume.HIGHEST, volumeFromContext);
  }

  @Test
  void rejectTraceForSnapshottingWhenDecisionLeftUndecidedButTraceIsRejected() {
    selector.toggle(State.OFF);

    var spanContext = Snapshotting.spanContext().remote().build();
    var parentSpan = Span.wrap(spanContext);
    var context = Context.root().with(parentSpan);

    var contextFromPropagator = propagator.extract(context, carrier, carrier);

    var volumeFromContext = Volume.from(contextFromPropagator);
    assertEquals(Volume.OFF, volumeFromContext);
  }

  static class ToggleableSnapshotSelector implements SnapshotSelector {
    enum State {
      ON,
      OFF
    }

    private State state = State.ON;

    @Override
    public boolean select(Context context) {
      return state == State.ON;
    }

    void toggle(State state) {
      this.state = state;
    }
  }
}
