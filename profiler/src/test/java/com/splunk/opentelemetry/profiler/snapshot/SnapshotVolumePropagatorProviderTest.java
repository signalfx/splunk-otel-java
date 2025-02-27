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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.splunk.opentelemetry.profiler.Configuration;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

  @ParameterizedTest
  @ValueSource(doubles = { 1.0, 0.75, 0.5, 0.25, 0.0 })
  void configureSnapshotSelectionRateFromConfigProperties(double selectionRate) throws Exception {
    var propagator = provider.getPropagator(DefaultConfigProperties.create(Map.of(
        Configuration.CONFIG_KEY_SNAPSHOT_SELECTION_RATE, String.valueOf(selectionRate)
    )));
    var actualSelectionRate = reflectivelyGetSelectionRate(propagator);

    assertEquals(selectionRate, actualSelectionRate);
  }

  private double reflectivelyGetSelectionRate(TextMapPropagator propagator) throws Exception {
    Field selectorField = SnapshotVolumePropagator.class.getDeclaredField("selector");
    selectorField.setAccessible(true);
    var selector = selectorField.get(propagator);

    Field selectionRateField = ProbabilisticSnapshotSelector.class.getDeclaredField("selectionRate");
    selectionRateField.setAccessible(true);
    return (double)selectionRateField.get(selector);
  }
}
