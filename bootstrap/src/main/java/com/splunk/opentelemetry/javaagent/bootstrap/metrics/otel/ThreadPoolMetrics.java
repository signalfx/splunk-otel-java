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

package com.splunk.opentelemetry.javaagent.bootstrap.metrics.otel;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterBuilder;
import io.opentelemetry.api.metrics.ObservableDoubleCounter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import io.opentelemetry.instrumentation.api.internal.EmbeddedInstrumentationProperties;
import java.util.function.Supplier;

public final class ThreadPoolMetrics {

  static final AttributeKey<String> EXECUTOR_NAME = stringKey("executor.name");
  static final AttributeKey<String> EXECUTOR_TYPE = stringKey("executor.type");

  public static ThreadPoolMetrics create(
      OpenTelemetry openTelemetry,
      String instrumentationName,
      String executorType,
      String executorName) {

    MeterBuilder meterBuilder = openTelemetry.getMeterProvider().meterBuilder(instrumentationName);
    String version = EmbeddedInstrumentationProperties.findVersion(instrumentationName);
    if (version != null) {
      meterBuilder.setInstrumentationVersion(version);
    }

    return new ThreadPoolMetrics(
        meterBuilder.build(),
        Attributes.of(EXECUTOR_TYPE, executorType, EXECUTOR_NAME, executorName));
  }

  private final Meter meter;
  private final Attributes attributes;

  private ThreadPoolMetrics(Meter meter, Attributes attributes) {
    this.meter = meter;
    this.attributes = attributes;
  }

  public ObservableDoubleGauge currentThreads(Supplier<Number> supplier) {
    return meter
        .gaugeBuilder("executor.threads")
        .setUnit("threads")
        .setDescription("The current number of threads in the pool.")
        .buildWithCallback(
            measurement -> measurement.record(supplier.get().doubleValue(), attributes));
  }

  public ObservableDoubleGauge activeThreads(Supplier<Number> supplier) {
    return meter
        .gaugeBuilder("executor.threads.active")
        .setUnit("threads")
        .setDescription("The number of threads that are currently busy.")
        .buildWithCallback(
            measurement -> measurement.record(supplier.get().doubleValue(), attributes));
  }

  public ObservableDoubleGauge idleThreads(Supplier<Number> supplier) {
    return meter
        .gaugeBuilder("executor.threads.idle")
        .setUnit("threads")
        .setDescription("The number of threads that are currently idle.")
        .buildWithCallback(
            measurement -> measurement.record(supplier.get().doubleValue(), attributes));
  }

  public ObservableDoubleGauge coreThreads(Supplier<Number> supplier) {
    return meter
        .gaugeBuilder("executor.threads.core")
        .setUnit("threads")
        .setDescription(
            "Core thread pool size - the number of threads that are always kept in the pool.")
        .buildWithCallback(
            measurement -> measurement.record(supplier.get().doubleValue(), attributes));
  }

  public ObservableDoubleGauge maxThreads(Supplier<Number> supplier) {
    return meter
        .gaugeBuilder("executor.threads.max")
        .setUnit("threads")
        .setDescription("The maximum number of threads in the pool.")
        .buildWithCallback(
            measurement -> measurement.record(supplier.get().doubleValue(), attributes));
  }

  public ObservableDoubleCounter tasksSubmitted(Supplier<Number> supplier) {
    return meter
        .counterBuilder("executor.tasks.submitted")
        .ofDoubles()
        .setUnit("tasks")
        .setDescription("The total number of tasks that were submitted to this executor.")
        .buildWithCallback(
            measurement -> measurement.record(supplier.get().doubleValue(), attributes));
  }

  public ObservableDoubleCounter tasksCompleted(Supplier<Number> supplier) {
    return meter
        .counterBuilder("executor.tasks.completed")
        .ofDoubles()
        .setUnit("tasks")
        .setDescription("The total number of tasks completed by this executor.")
        .buildWithCallback(
            measurement -> measurement.record(supplier.get().doubleValue(), attributes));
  }
}
