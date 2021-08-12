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

import application.io.micrometer.core.instrument.FunctionTimer;
import java.util.concurrent.TimeUnit;

class ApplicationFunctionTimer extends ApplicationMeter implements FunctionTimer {
  private final io.micrometer.core.instrument.FunctionTimer agentFunctionTimer;

  ApplicationFunctionTimer(Id id, io.micrometer.core.instrument.FunctionTimer agentFunctionTimer) {
    super(id, agentFunctionTimer);
    this.agentFunctionTimer = agentFunctionTimer;
  }

  @Override
  public double count() {
    return agentFunctionTimer.count();
  }

  @Override
  public double totalTime(TimeUnit timeUnit) {
    return agentFunctionTimer.totalTime(timeUnit);
  }

  @Override
  public TimeUnit baseTimeUnit() {
    return agentFunctionTimer.baseTimeUnit();
  }
}
