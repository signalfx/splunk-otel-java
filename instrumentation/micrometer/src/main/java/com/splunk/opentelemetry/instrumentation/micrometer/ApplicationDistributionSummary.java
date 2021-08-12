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

import application.io.micrometer.core.instrument.DistributionSummary;
import application.io.micrometer.core.instrument.distribution.HistogramSnapshot;

class ApplicationDistributionSummary extends ApplicationMeter implements DistributionSummary {
  private final io.micrometer.core.instrument.DistributionSummary agentDistributionSummary;

  ApplicationDistributionSummary(
      Id id, io.micrometer.core.instrument.DistributionSummary agentDistributionSummary) {
    super(id, agentDistributionSummary);
    this.agentDistributionSummary = agentDistributionSummary;
  }

  @Override
  public void record(double v) {
    agentDistributionSummary.record(v);
  }

  @Override
  public long count() {
    return agentDistributionSummary.count();
  }

  @Override
  public double totalAmount() {
    return agentDistributionSummary.totalAmount();
  }

  @Override
  public double max() {
    return agentDistributionSummary.max();
  }

  @Override
  public HistogramSnapshot takeSnapshot() {
    return toApplication(agentDistributionSummary.takeSnapshot());
  }
}
