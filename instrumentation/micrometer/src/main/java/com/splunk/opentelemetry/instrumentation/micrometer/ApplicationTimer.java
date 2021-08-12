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

import static com.splunk.opentelemetry.instrumentation.micrometer.Bridging.toApplication;

import application.io.micrometer.core.instrument.Timer;
import application.io.micrometer.core.instrument.distribution.HistogramSnapshot;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class ApplicationTimer extends ApplicationMeter implements Timer {
  private final io.micrometer.core.instrument.Timer agentTimer;

  ApplicationTimer(Id id, io.micrometer.core.instrument.Timer agentTimer) {
    super(id, agentTimer);
    this.agentTimer = agentTimer;
  }

  @Override
  public void record(long l, TimeUnit timeUnit) {
    agentTimer.record(l, timeUnit);
  }

  @Override
  public <T> T record(Supplier<T> supplier) {
    return agentTimer.record(supplier);
  }

  @Override
  public <T> T recordCallable(Callable<T> callable) throws Exception {
    return agentTimer.recordCallable(callable);
  }

  @Override
  public void record(Runnable runnable) {
    agentTimer.record(runnable);
  }

  @Override
  public long count() {
    return agentTimer.count();
  }

  @Override
  public double totalTime(TimeUnit timeUnit) {
    return agentTimer.totalTime(timeUnit);
  }

  @Override
  public double max(TimeUnit timeUnit) {
    return agentTimer.max(timeUnit);
  }

  @Override
  public TimeUnit baseTimeUnit() {
    return agentTimer.baseTimeUnit();
  }

  @Override
  public HistogramSnapshot takeSnapshot() {
    return toApplication(agentTimer.takeSnapshot());
  }
}
