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

import com.splunk.opentelemetry.profiler.snapshot.registry.TraceRegistry;
import com.splunk.opentelemetry.profiler.snapshot.registry.TraceRegistryHolder;
import java.time.Duration;

class SnapshotProfilingSdkCustomizerBuilder {
  private TraceRegistry registry = TraceRegistryHolder.getTraceRegistry();
  private StackTraceSampler sampler = new ObservableStackTraceSampler();
  private ContextStorageWrapper contextStorageWrapper = new ResettingContextStorageWrapper();

  SnapshotProfilingSdkCustomizerBuilder with(TraceRegistry registry) {
    this.registry = registry;
    return this;
  }

  SnapshotProfilingSdkCustomizer real() {
    return new SnapshotProfilingSdkCustomizer();
  }

  SnapshotProfilingSdkCustomizerBuilder withRealStackTraceSampler() {
    return withRealStackTraceSampler(Duration.ofMillis(20));
  }

  SnapshotProfilingSdkCustomizerBuilder withRealStackTraceSampler(Duration samplingPeriod) {
    return with(
        new PeriodicStackTraceSampler(StagingArea.SUPPLIER, SpanTracker.SUPPLIER, samplingPeriod));
  }

  SnapshotProfilingSdkCustomizerBuilder with(StackTraceSampler sampler) {
    StackTraceSampler.SUPPLIER.configure(sampler);
    this.sampler = sampler;
    return this;
  }

  SnapshotProfilingSdkCustomizerBuilder withRealStagingArea() {
    return withRealStagingArea(Duration.ofMillis(200));
  }

  SnapshotProfilingSdkCustomizerBuilder withRealStagingArea(Duration exportPeriod) {
    return with(
        new PeriodicallyExportingStagingArea(StackTraceExporter.SUPPLIER, exportPeriod, 10));
  }

  SnapshotProfilingSdkCustomizerBuilder with(StagingArea stagingArea) {
    StagingArea.SUPPLIER.configure(stagingArea);
    return this;
  }

  SnapshotProfilingSdkCustomizerBuilder with(ContextStorageWrapper contextStorageWrapper) {
    this.contextStorageWrapper = contextStorageWrapper;
    return this;
  }

  SnapshotProfilingSdkCustomizer build() {
    return new SnapshotProfilingSdkCustomizer(registry, sampler, contextStorageWrapper);
  }
}
