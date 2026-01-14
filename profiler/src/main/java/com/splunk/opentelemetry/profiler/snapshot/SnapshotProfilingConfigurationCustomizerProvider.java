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

import static io.opentelemetry.api.incubator.config.DeclarativeConfigProperties.empty;
import static io.opentelemetry.sdk.autoconfigure.AutoConfigureUtil.getDistributionConfig;

import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.TracerProviderModel;

@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class SnapshotProfilingConfigurationCustomizerProvider
    implements DeclarativeConfigurationCustomizerProvider {
  private final TraceRegistry registry;
  private final ContextStorageWrapper contextStorageWrapper;

  public SnapshotProfilingConfigurationCustomizerProvider() {
    this(TraceRegistryHolder.getTraceRegistry(), new ContextStorageWrapper());
  }

  public void customize(DeclarativeConfigurationCustomizer configurationCustomizer) {
    configurationCustomizer.addModelCustomizer(this::customizeModel);
  }

  @VisibleForTesting
  OpenTelemetryConfigurationModel customizeModel(OpenTelemetryConfigurationModel model) {
    if (isSnapshotProfilingEnabled(model)) {
      initActiveSpansTracking();
      initStackTraceSampler(model);
      addShutdownHookSpanProcessor(model);
    }
    return model;
  }

  private void initStackTraceSampler(OpenTelemetryConfigurationModel model) {
    SnapshotProfilingDeclarativeConfiguration snapshotProfilingConfig =
        getSnapshotProfilingConfig(model);

    StackTraceSamplerInitializer.setupStackTraceSampler(snapshotProfilingConfig);
  }

  private void addShutdownHookSpanProcessor(OpenTelemetryConfigurationModel model) {
    if (model.getTracerProvider() == null) {
      model.withTracerProvider(new TracerProviderModel());
    }
    model
        .getTracerProvider()
        .getProcessors()
        .add(
            new SpanProcessorModel()
                .withAdditionalProperty(SdkShutdownHookComponentProvider.NAME, null));
  }

  private void initActiveSpansTracking() {
    contextStorageWrapper.wrapContextStorage(registry);
  }

  private static SnapshotProfilingDeclarativeConfiguration getSnapshotProfilingConfig(
      OpenTelemetryConfigurationModel model) {
    DeclarativeConfigProperties profilingConfig =
        getDistributionConfig(model)
            .getStructured("splunk", empty())
            .getStructured("profiling", empty());
    return new SnapshotProfilingDeclarativeConfiguration(profilingConfig);
  }

  private static boolean isSnapshotProfilingEnabled(OpenTelemetryConfigurationModel model) {
    SnapshotProfilingDeclarativeConfiguration snapshotProfilingConfig =
        getSnapshotProfilingConfig(model);
    return snapshotProfilingConfig.isEnabled();
  }

  @VisibleForTesting
  SnapshotProfilingConfigurationCustomizerProvider(
      TraceRegistry registry, ContextStorageWrapper contextStorageWrapper) {
    this.registry = registry;
    this.contextStorageWrapper = contextStorageWrapper;
  }
}
