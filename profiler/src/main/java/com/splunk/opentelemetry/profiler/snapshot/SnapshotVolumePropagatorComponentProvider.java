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

import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;

@AutoService(ComponentProvider.class)
public class SnapshotVolumePropagatorComponentProvider implements ComponentProvider {
  private static final double MAX_SELECTION_PROBABILITY = 0.1;
  private static final String SELECTION_PROBABILITY_PROPERTY = "snapshot_selection_probability";

  @Override
  public Class<TextMapPropagator> getType() {
    return TextMapPropagator.class;
  }

  @Override
  public String getName() {
    return "splunk_snapshot_volume";
  }

  @Override
  public TextMapPropagator create(DeclarativeConfigProperties propagatorProperties) {
    double selectionProbability =
        propagatorProperties.getDouble(SELECTION_PROBABILITY_PROPERTY, 0.01);

    validateConfiguration(selectionProbability);

    return new SnapshotVolumePropagator(selector(selectionProbability));
  }

  private static void validateConfiguration(double selectionProbability) {
    if ((selectionProbability <= 0) || (selectionProbability > MAX_SELECTION_PROBABILITY)) {
      throw new ConfigurationException(
          "Invalid value of "
              + SELECTION_PROBABILITY_PROPERTY
              + " property: "
              + selectionProbability
              + " - should be in range (0, "
              + MAX_SELECTION_PROBABILITY
              + "]");
    }
  }

  private SnapshotSelector selector(double selectionProbability) {
    return new TraceIdBasedSnapshotSelector(selectionProbability)
        .or(new ProbabilisticSnapshotSelector(selectionProbability));
  }
}
