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

import application.io.micrometer.core.instrument.FunctionCounter;

class ApplicationFunctionCounter extends ApplicationMeter implements FunctionCounter {
  private final io.micrometer.core.instrument.FunctionCounter agentFunctionCounter;

  ApplicationFunctionCounter(
      Id id, io.micrometer.core.instrument.FunctionCounter agentFunctionCounter) {
    super(id, agentFunctionCounter);
    this.agentFunctionCounter = agentFunctionCounter;
  }

  @Override
  public double count() {
    return agentFunctionCounter.count();
  }
}
