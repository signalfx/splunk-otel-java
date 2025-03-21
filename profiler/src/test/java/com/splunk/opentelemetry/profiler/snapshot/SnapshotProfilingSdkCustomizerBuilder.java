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

import java.time.Duration;

class SnapshotProfilingSdkCustomizerBuilder {
  private TraceRegistry registry = new TraceRegistry();
  private StackTraceSampler sampler = new ObservableStackTraceSampler();

  SnapshotProfilingSdkCustomizerBuilder with(TraceRegistry registry) {
    this.registry = registry;
    return this;
  }

  SnapshotProfilingSdkCustomizerBuilder withRealStackTraceSampler() {
    return with(
        new ScheduledExecutorStackTraceSampler(
            new AccumulatingStagingArea(StackTraceExporterProvider.INSTANCE),
            Duration.ofMillis(20)));
  }

  SnapshotProfilingSdkCustomizerBuilder with(StackTraceSampler sampler) {
    this.sampler = sampler;
    return this;
  }

  SnapshotProfilingSdkCustomizer build() {
    return new SnapshotProfilingSdkCustomizer(registry, sampler);
  }
}
