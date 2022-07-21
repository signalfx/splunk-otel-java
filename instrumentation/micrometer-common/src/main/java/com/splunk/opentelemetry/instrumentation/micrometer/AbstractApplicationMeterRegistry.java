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

package com.splunk.opentelemetry.instrumentation.micrometer;

import static com.splunk.opentelemetry.instrumentation.micrometer.Bridging.toAgent;
import static com.splunk.opentelemetry.instrumentation.micrometer.Bridging.toAgentMeasurements;
import static com.splunk.opentelemetry.instrumentation.micrometer.Bridging.toAgentTags;
import static com.splunk.opentelemetry.instrumentation.micrometer.Bridging.toDuration;
import static com.splunk.opentelemetry.instrumentation.micrometer.Bridging.toDurations;

import application.io.micrometer.core.instrument.Counter;
import application.io.micrometer.core.instrument.DistributionSummary;
import application.io.micrometer.core.instrument.FunctionCounter;
import application.io.micrometer.core.instrument.FunctionTimer;
import application.io.micrometer.core.instrument.Gauge;
import application.io.micrometer.core.instrument.Measurement;
import application.io.micrometer.core.instrument.Meter;
import application.io.micrometer.core.instrument.MeterRegistry;
import application.io.micrometer.core.instrument.Timer;
import application.io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import application.io.micrometer.core.instrument.distribution.pause.PauseDetector;
import io.opentelemetry.javaagent.bootstrap.internal.InstrumentationConfig;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

public abstract class AbstractApplicationMeterRegistry extends MeterRegistry {

  protected io.micrometer.core.instrument.MeterRegistry agentRegistry;

  public AbstractApplicationMeterRegistry(
      io.micrometer.core.instrument.Clock agentClock,
      io.micrometer.core.instrument.MeterRegistry agentRegistry) {
    super(new ApplicationClock(agentClock));
    this.agentRegistry = agentRegistry;
  }

  @Override
  protected <T> Gauge newGauge(Meter.Id id, T t, ToDoubleFunction<T> toDoubleFunction) {
    io.micrometer.core.instrument.Gauge agentGauge =
        io.micrometer.core.instrument.Gauge.builder(id.getName(), t, toDoubleFunction)
            .tags(toAgentTags(id.getTagsAsIterable()))
            .baseUnit(id.getBaseUnit())
            .description(id.getDescription())
            .synthetic(toAgent(id.syntheticAssociation()))
            .register(agentRegistry);
    return new ApplicationGauge(id, agentGauge);
  }

  @Override
  protected Counter newCounter(Meter.Id id) {
    io.micrometer.core.instrument.Counter agentCounter =
        io.micrometer.core.instrument.Counter.builder(id.getName())
            .tags(toAgentTags(id.getTagsAsIterable()))
            .baseUnit(id.getBaseUnit())
            .description(id.getDescription())
            .register(agentRegistry);
    return new ApplicationCounter(id, agentCounter);
  }

  @Override
  protected Timer newTimer(
      Meter.Id id, DistributionStatisticConfig distributionStats, PauseDetector pauseDetector) {
    io.micrometer.core.instrument.Timer agentTimer =
        io.micrometer.core.instrument.Timer.builder(id.getName())
            .tags(toAgentTags(id.getTagsAsIterable()))
            .description(id.getDescription())
            .publishPercentiles(distributionStats.getPercentiles())
            .percentilePrecision(distributionStats.getPercentilePrecision())
            .publishPercentileHistogram(distributionStats.isPublishingHistogram())
            .serviceLevelObjectives(
                toDurations(distributionStats.getServiceLevelObjectiveBoundaries()))
            .minimumExpectedValue(toDuration(distributionStats.getMinimumExpectedValueAsDouble()))
            .maximumExpectedValue(toDuration(distributionStats.getMaximumExpectedValueAsDouble()))
            .distributionStatisticExpiry(distributionStats.getExpiry())
            .distributionStatisticBufferLength(distributionStats.getBufferLength())
            .pauseDetector(toAgent(pauseDetector))
            .register(agentRegistry);
    return new ApplicationTimer(id, agentTimer);
  }

  @Override
  protected DistributionSummary newDistributionSummary(
      Meter.Id id, DistributionStatisticConfig distributionStats, double scale) {
    io.micrometer.core.instrument.DistributionSummary agentDistributionSummary =
        io.micrometer.core.instrument.DistributionSummary.builder(id.getName())
            .tags(toAgentTags(id.getTagsAsIterable()))
            .description(id.getDescription())
            .baseUnit(id.getBaseUnit())
            .publishPercentiles(distributionStats.getPercentiles())
            .percentilePrecision(distributionStats.getPercentilePrecision())
            .publishPercentileHistogram(distributionStats.isPublishingHistogram())
            .serviceLevelObjectives(distributionStats.getServiceLevelObjectiveBoundaries())
            .minimumExpectedValue(distributionStats.getMinimumExpectedValueAsDouble())
            .maximumExpectedValue(distributionStats.getMaximumExpectedValueAsDouble())
            .distributionStatisticExpiry(distributionStats.getExpiry())
            .distributionStatisticBufferLength(distributionStats.getBufferLength())
            .scale(scale)
            .register(agentRegistry);
    return new ApplicationDistributionSummary(id, agentDistributionSummary);
  }

  @Override
  protected Meter newMeter(Meter.Id id, Meter.Type type, Iterable<Measurement> iterable) {
    io.micrometer.core.instrument.Meter agentMeter =
        io.micrometer.core.instrument.Meter.builder(
                id.getName(), toAgent(id.getType()), toAgentMeasurements(iterable))
            .tags(toAgentTags(id.getTagsAsIterable()))
            .baseUnit(id.getBaseUnit())
            .description(id.getDescription())
            .register(agentRegistry);
    return new ApplicationMeter(id, agentMeter);
  }

  @Override
  protected <T> FunctionTimer newFunctionTimer(
      Meter.Id id,
      T t,
      ToLongFunction<T> toLongFunction,
      ToDoubleFunction<T> toDoubleFunction,
      TimeUnit timeUnit) {
    io.micrometer.core.instrument.FunctionTimer agentTimer =
        io.micrometer.core.instrument.FunctionTimer.builder(
                id.getName(), t, toLongFunction, toDoubleFunction, timeUnit)
            .tags(toAgentTags(id.getTagsAsIterable()))
            .description(id.getDescription())
            .register(agentRegistry);
    return new ApplicationFunctionTimer(id, agentTimer);
  }

  @Override
  protected <T> FunctionCounter newFunctionCounter(
      Meter.Id id, T t, ToDoubleFunction<T> toDoubleFunction) {
    io.micrometer.core.instrument.FunctionCounter agentCounter =
        io.micrometer.core.instrument.FunctionCounter.builder(id.getName(), t, toDoubleFunction)
            .tags(toAgentTags(id.getTagsAsIterable()))
            .baseUnit(id.getBaseUnit())
            .description(id.getDescription())
            .register(agentRegistry);
    return new ApplicationFunctionCounter(id, agentCounter);
  }

  @Override
  public Meter remove(Meter.Id mappedId) {
    agentRegistry.remove(toAgent(mappedId));
    return super.remove(mappedId);
  }

  @Override
  protected TimeUnit getBaseTimeUnit() {
    // same as StepMeterRegistry
    return TimeUnit.SECONDS;
  }

  @Override
  protected DistributionStatisticConfig defaultHistogramConfig() {
    // same as StepMeterRegistry - which is a bit hacky because we can't easily access the agent
    // registry config object
    Duration exportInterval =
        InstrumentationConfig.get()
            .getDuration("splunk.metrics.export.interval", Duration.ofSeconds(30));
    return DistributionStatisticConfig.builder()
        .expiry(exportInterval)
        .build()
        .merge(DistributionStatisticConfig.DEFAULT);
  }
}
