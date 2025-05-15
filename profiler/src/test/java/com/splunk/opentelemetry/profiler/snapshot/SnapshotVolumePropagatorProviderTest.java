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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SnapshotVolumePropagatorProviderTest {
  private final SnapshotVolumePropagatorProvider provider = new SnapshotVolumePropagatorProvider();

  @Test
  void provideSnapshotVolumePropagator() {
    var propagator = provider.getPropagator(DefaultConfigProperties.create(Collections.emptyMap()));
    assertInstanceOf(SnapshotVolumePropagator.class, propagator);
  }

  @Test
  void name() {
    assertEquals("splunk-snapshot", provider.getName());
  }

  /** Has a roughly 0.0025% chance to fail. */
  @Test
  void probabilisticSelectorIsConfigured() {
    var properties =
        DefaultConfigProperties.create(Map.of("splunk.snapshot.selection.rate", "0.10"));

    var carrier = new ObservableCarrier();
    var propagator = provider.getPropagator(properties);

    var volumes = new ArrayList<Volume>();
    for (int i = 0; i < 100; i++) {
      var contextFromPropagator = propagator.extract(Context.root(), carrier, carrier);
      var volume = Volume.from(contextFromPropagator);
      volumes.add(volume);
    }

    assertThat(volumes).contains(Volume.HIGHEST);
  }

  @ParameterizedTest
  @MethodSource("traceIdsToSelect")
  void traceIdSelectorIsConfigured(String traceId) {
    var properties =
        DefaultConfigProperties.create(Map.of("splunk.snapshot.selection.rate", "0.10"));

    var remoteSpanContext = Snapshotting.spanContext().withTraceId(traceId).remote().build();
    var remoteParentSpan = Span.wrap(remoteSpanContext);
    var context = Context.root().with(remoteParentSpan);

    var carrier = new ObservableCarrier();
    var propagator = provider.getPropagator(properties);
    var contextFromPropagator = propagator.extract(context, carrier, carrier);
    var volume = Volume.from(contextFromPropagator);

    assertEquals(Volume.HIGHEST, volume);
  }

  private static Stream<String> traceIdsToSelect() {
    return IntStream.range(-10, 11).mapToObj(SpecialTraceIds::forPercentile);
  }
}
