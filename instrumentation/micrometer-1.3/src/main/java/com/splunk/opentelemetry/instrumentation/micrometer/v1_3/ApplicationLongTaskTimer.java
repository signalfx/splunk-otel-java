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

package com.splunk.opentelemetry.instrumentation.micrometer.v1_3;

import application.io.micrometer.core.instrument.LongTaskTimer;
import com.splunk.opentelemetry.instrumentation.micrometer.ApplicationMeter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

class ApplicationLongTaskTimer extends ApplicationMeter implements LongTaskTimer {

  private final io.micrometer.core.instrument.LongTaskTimer agentTimer;

  // micrometer 1.3 uses a similar pattern for assigning ids to tasks
  private final ConcurrentMap<Long, ApplicationSample> activeTasks = new ConcurrentHashMap<>();
  private final AtomicLong nextTask = new AtomicLong(0L);

  ApplicationLongTaskTimer(Id id, io.micrometer.core.instrument.LongTaskTimer agentTimer) {
    super(id, agentTimer);
    this.agentTimer = agentTimer;
  }

  @Override
  public Sample start() {
    ApplicationSample task =
        new ApplicationSample(agentTimer.start(), this, nextTask.getAndIncrement());
    activeTasks.put(task.id, task);
    return task;
  }

  @Override
  public long stop(long taskId) {
    ApplicationSample task = activeTasks.remove(taskId);
    if (task == null) {
      return -1L;
    }
    return task.agentSample.stop();
  }

  @Override
  public double duration(long taskId, TimeUnit unit) {
    ApplicationSample task = activeTasks.get(taskId);
    if (task == null) {
      return -1L;
    }
    return task.agentSample.duration(unit);
  }

  @Override
  public double duration(TimeUnit timeUnit) {
    return agentTimer.duration(timeUnit);
  }

  @Override
  public int activeTasks() {
    return agentTimer.activeTasks();
  }

  private final class ApplicationSample extends Sample {
    private final io.micrometer.core.instrument.LongTaskTimer.Sample agentSample;
    private final long id;

    private ApplicationSample(
        io.micrometer.core.instrument.LongTaskTimer.Sample agentSample,
        LongTaskTimer timer,
        long id) {
      super(timer, id);
      this.agentSample = agentSample;
      this.id = id;
    }
  }
}
