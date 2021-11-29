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

package com.splunk.opentelemetry.instrumentation.micrometer.v1_5;

import static com.splunk.opentelemetry.instrumentation.micrometer.Bridging.toApplication;

import application.io.micrometer.core.instrument.LongTaskTimer;
import application.io.micrometer.core.instrument.distribution.HistogramSnapshot;
import com.splunk.opentelemetry.instrumentation.micrometer.ApplicationMeter;
import java.util.concurrent.TimeUnit;

class ApplicationLongTaskTimer extends ApplicationMeter implements LongTaskTimer {
  private final io.micrometer.core.instrument.LongTaskTimer agentTimer;

  ApplicationLongTaskTimer(Id id, io.micrometer.core.instrument.LongTaskTimer agentTimer) {
    super(id, agentTimer);
    this.agentTimer = agentTimer;
  }

  @Override
  public Sample start() {
    return new ApplicationSample(agentTimer.start());
  }

  @Override
  public double duration(TimeUnit timeUnit) {
    return agentTimer.duration(timeUnit);
  }

  @Override
  public int activeTasks() {
    return agentTimer.activeTasks();
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

  private static final class ApplicationSample extends Sample {
    private final io.micrometer.core.instrument.LongTaskTimer.Sample agentSample;

    private ApplicationSample(io.micrometer.core.instrument.LongTaskTimer.Sample agentSample) {
      this.agentSample = agentSample;
    }

    @Override
    public long stop() {
      return agentSample.stop();
    }

    @Override
    public double duration(TimeUnit timeUnit) {
      return agentSample.duration(timeUnit);
    }
  }
}
