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

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.Collection;
import java.util.Collections;

class SnapshotVolumePropagator implements TextMapPropagator {
  private final SnapshotSelector selector;

  SnapshotVolumePropagator(SnapshotSelector selector) {
    this.selector = selector;
  }

  @Override
  public Collection<String> fields() {
    return Collections.emptyList();
  }

  @Override
  public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {}

  /**
   * Make a decision whether a trace should be selected for snapshotting if --and only if -- that
   * decision has not yet been made by an upstream service. Ideally a snapshotting decision will
   * have been made only at trace root, however there are many scenarios (e.g. RUM, or language
   * agents without snapshot support) where a trace will begin without a decision. In those
   * instances we would like to snapshot as much of the trace as possible.
   *
   * <p>Not seen here in the introduction of {@link TraceIdBasedSnapshotSelector} which will
   * deterministically select a trace based on the Trace ID value so that all participating and
   * capable agents can make the same snapshotting decision, if necessary.
   */
  @Override
  public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
    Volume volume = Volume.from(context);
    if (volume != Volume.UNSPECIFIED) {
      return context;
    }

    volume = selector.select(context) ? Volume.HIGHEST : Volume.OFF;
    return context.with(volume);
  }
}
