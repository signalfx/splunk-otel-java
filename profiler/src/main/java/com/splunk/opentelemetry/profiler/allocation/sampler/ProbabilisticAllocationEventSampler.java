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

package com.splunk.opentelemetry.profiler.allocation.sampler;

import java.util.function.BiConsumer;

public class ProbabilisticAllocationEventSampler implements AllocationEventSampler {
  private final double probability;
  private final String probabilityString;

  public ProbabilisticAllocationEventSampler(double probability) {
    if (probability < 0 || probability > 1) {
      throw new IllegalArgumentException("Invalid sampling probability " + probability);
    }
    this.probability = probability;
    this.probabilityString = String.valueOf(probability);
  }

  @Override
  public boolean shouldSample() {
    return Math.random() < probability;
  }

  @Override
  public void addAttributes(
      BiConsumer<String, String> stringAttributeAdder,
      BiConsumer<String, Long> longAttributeAdder) {
    stringAttributeAdder.accept("sampler.name", "Probabilistic sampler");
    stringAttributeAdder.accept("sampler.probability", probabilityString);
  }
}
