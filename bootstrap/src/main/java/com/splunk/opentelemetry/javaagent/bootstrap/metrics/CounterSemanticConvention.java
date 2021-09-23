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

package com.splunk.opentelemetry.javaagent.bootstrap.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

public final class CounterSemanticConvention implements MeterSemanticConvention {
  private final String name;
  private final String baseUnit;

  private CounterSemanticConvention(String name, String baseUnit) {
    this.name = name;
    this.baseUnit = baseUnit;
  }

  static CounterSemanticConvention counter(String name, String baseUnit) {
    return new CounterSemanticConvention(name, baseUnit);
  }

  public Counter create(Tags additionalTags) {
    return Counter.builder(name)
        .tags(GlobalMetricsTags.get())
        .tags(additionalTags)
        .baseUnit(baseUnit)
        .register(Metrics.globalRegistry);
  }

  public FunctionCounter create(Tags additionalTags, Supplier<Number> function) {
    return FunctionCounter.builder(name, function, f -> function.get().doubleValue())
        .tags(GlobalMetricsTags.get())
        .tags(additionalTags)
        .baseUnit(baseUnit)
        .register(Metrics.globalRegistry);
  }

  public <T> FunctionCounter create(Tags additionalTags, T obj, ToDoubleFunction<T> function) {
    return create(additionalTags, () -> function.applyAsDouble(obj));
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String baseUnit() {
    return baseUnit;
  }
}
