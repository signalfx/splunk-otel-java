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

import static com.splunk.opentelemetry.instrumentation.micrometer.Bridging.toApplicationMeasurements;

import application.io.micrometer.core.instrument.Measurement;
import application.io.micrometer.core.instrument.Meter;

class ApplicationMeter implements Meter {
  final Id id;
  final io.micrometer.core.instrument.Meter agentMeter;
  volatile Iterable<Measurement> measurements = null;

  ApplicationMeter(Id id, io.micrometer.core.instrument.Meter agentMeter) {
    this.id = id;
    this.agentMeter = agentMeter;
  }

  @Override
  public Id getId() {
    return id;
  }

  @Override
  public Iterable<Measurement> measure() {
    // lazy initialize measurements - they're always the same functions, no need to compute them
    // each time
    if (measurements == null) {
      measurements = toApplicationMeasurements(agentMeter.measure());
    }
    return measurements;
  }

  @Override
  public void close() {
    agentMeter.close();
  }
}
