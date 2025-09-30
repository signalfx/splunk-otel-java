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

import static com.splunk.opentelemetry.profiler.util.ProfilerDeclarativeConfigUtil.isProfilerEnabled;

import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.instrumentation.config.bridge.DeclarativeConfigPropertiesBridgeBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.SdkConfigProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.BaggagePropagatorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalLanguageSpecificInstrumentationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.PropagatorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.TextMapPropagatorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.TraceContextPropagatorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.TracerProviderModel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * This class combines declarative config compatible port of:
 * - SnapshotProfilingSdkCustomizer
 */
@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class SnapshotProfilingConfigurationCustomizerProvider
    implements DeclarativeConfigurationCustomizerProvider {
  private final TraceRegistry registry;
  private final Function<ConfigProperties, StackTraceSampler> samplerProvider;
  private final ContextStorageWrapper contextStorageWrapper;

  public SnapshotProfilingConfigurationCustomizerProvider() {
    this(new TraceRegistry(), stackTraceSamplerProvider(), new ContextStorageWrapper());
  }

  public void customize(DeclarativeConfigurationCustomizer configurationCustomizer) {
    configurationCustomizer.addModelCustomizer(
        model -> {
          if (isProfilerEnabled(model)) {
            customizePropagators(model);
            customizeSpanProcessor(model);
            setupStackTraceSampler(model);
            addShutdownHookSpanProcessor(model);
            initActiveSpansTracking();
          }
          return model;
        });
  }

  private void customizePropagators(OpenTelemetryConfigurationModel model) {
    // TODO: How we should handle "none" ? (see
    // SnapshotProfilingSdkCustomizer::autoConfigureSnapshotVolumePropagator which )
    PropagatorModel propagator = model.getPropagator();
    if (propagator == null) { // TODO: make an utility for this kind of code ( getOrAddXXX(model) )
      propagator = new PropagatorModel();
      model.withPropagator(propagator);
    }

    List<TextMapPropagatorModel> propagatorsComposite = propagator.getComposite();
    if (propagatorsComposite == null) {
      propagatorsComposite = new ArrayList<>();
      propagator.withComposite(propagatorsComposite);
    }
    if (propagatorsComposite.isEmpty() &&
        (propagator.getCompositeList() == null || propagator.getCompositeList().isEmpty())) {
      propagatorsComposite.add(new TextMapPropagatorModel().withTracecontext(new TraceContextPropagatorModel()));
    }
    propagatorsComposite.add(new TextMapPropagatorModel().withBaggage(new BaggagePropagatorModel()));

    propagatorsComposite.add(getSnapshotVolumePropagator(model));
  }

  private static TextMapPropagatorModel getSnapshotVolumePropagator(
      OpenTelemetryConfigurationModel model) {
    TextMapPropagatorModel snapshotVolumePropagator = new TextMapPropagatorModel();
    Map<String, Object> config = null;
    if (model.getInstrumentationDevelopment() != null) {
      ExperimentalLanguageSpecificInstrumentationModel java = model.getInstrumentationDevelopment().getJava();
      if (java != null) {
        // Pass reference to existing java instrumentation config, so ComponentProvider can access these properties
        config = java.getAdditionalProperties();
      }
    }
    snapshotVolumePropagator.setAdditionalProperty(SnapshotVolumePropagatorComponentProvider.NAME, config);
    return snapshotVolumePropagator;
  }

  private void customizeSpanProcessor(OpenTelemetryConfigurationModel model) {
    SpanProcessorModel spanProcessor = new SpanProcessorModel().withAdditionalProperty(
        SnapshotProfilingSpanProcessorComponentProvider.NAME, null);
    SnapshotProfilingSpanProcessorComponentProvider.setTraceRegistry(registry);

    TracerProviderModel tracerProvider = model.getTracerProvider();
    if (tracerProvider == null) {
      tracerProvider = new TracerProviderModel();
      model.withTracerProvider(tracerProvider);
    }

    tracerProvider.getProcessors().add(spanProcessor);
  }

  private void setupStackTraceSampler(OpenTelemetryConfigurationModel model) {
    DeclarativeConfigProperties instrumentationConfig = SdkConfigProvider.create(model)
        .getInstrumentationConfig();
    ConfigProperties properties = new DeclarativeConfigPropertiesBridgeBuilder().buildFromInstrumentationConfig(instrumentationConfig);
    StackTraceSampler sampler = samplerProvider.apply(properties);
    ConfigurableSupplier<StackTraceSampler> supplier = StackTraceSampler.SUPPLIER;
    supplier.configure(sampler);
  }

  private void addShutdownHookSpanProcessor(OpenTelemetryConfigurationModel model) {
    if (model.getTracerProvider() == null) {
      model.withTracerProvider(new TracerProviderModel());
    }
    model.getTracerProvider().getProcessors().add(new SpanProcessorModel().withAdditionalProperty(SdkShutdownHookComponentProvider.NAME, null));
  }

  private void initActiveSpansTracking() {
    contextStorageWrapper.wrapContextStorage(registry);
  }

  private static Function<ConfigProperties, StackTraceSampler> stackTraceSamplerProvider() {
    return properties -> {
      Duration samplingPeriod = SnapshotProfilingConfiguration.getSamplingInterval(properties);
      StagingArea.SUPPLIER.configure(createStagingArea(properties));
      return new PeriodicStackTraceSampler(
          StagingArea.SUPPLIER, SpanTracker.SUPPLIER, samplingPeriod);
    };
  }

  private static StagingArea createStagingArea(ConfigProperties properties) {
    Duration interval = SnapshotProfilingConfiguration.getExportInterval(properties);
    int capacity = SnapshotProfilingConfiguration.getStagingCapacity(properties);
    return new PeriodicallyExportingStagingArea(StackTraceExporter.SUPPLIER, interval, capacity);
  }

  @VisibleForTesting
  SnapshotProfilingConfigurationCustomizerProvider(
      TraceRegistry registry,
      StackTraceSampler sampler,
      ContextStorageWrapper contextStorageWrapper) {
    this(registry, properties -> sampler, contextStorageWrapper);
  }

  private SnapshotProfilingConfigurationCustomizerProvider(
      TraceRegistry registry,
      Function<ConfigProperties, StackTraceSampler> samplerProvider,
      ContextStorageWrapper contextStorageWrapper) {
    this.registry = registry;
    this.samplerProvider = samplerProvider;
    this.contextStorageWrapper = contextStorageWrapper;
  }
}
