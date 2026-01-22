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
import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.incubator.config.DeclarativeConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;

@AutoService(ComponentProvider.class)
public class SnapshotVolumePropagatorComponentProvider implements ComponentProvider {
  static final String NAME = "splunk_snapshot_volume";

  @Override
  public Class<TextMapPropagator> getType() {
    return TextMapPropagator.class;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public TextMapPropagator create(DeclarativeConfigProperties propagatorProperties) {
    if (!SnapshotProfilingDeclarativeConfiguration.SUPPLIER.isConfigured()) {
      throw new DeclarativeConfigException("Snapshot profiling is not configured");
    }
    double selectionProbability =
        SnapshotProfilingDeclarativeConfiguration.SUPPLIER.get().getSnapshotSelectionProbability();

    return new SnapshotVolumePropagator(selector(selectionProbability));
  }

  @VisibleForTesting
  SnapshotSelector selector(double selectionProbability) {
    return new TraceIdBasedSnapshotSelector(selectionProbability)
        .or(new ProbabilisticSnapshotSelector(selectionProbability));
  }
}
