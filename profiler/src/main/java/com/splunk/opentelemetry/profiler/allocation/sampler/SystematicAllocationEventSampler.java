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
import jdk.jfr.consumer.RecordedEvent;

/** A sampler that samples every Nth element, where N is the sampling interval. */
public class SystematicAllocationEventSampler implements AllocationEventSampler {
  private static final String SAMPLER_NAME_KEY = "sampler.name";
  private static final String SAMPLER_INTERVAL_KEY = "sampler.interval";

  private final int samplingInterval;
  private int counter;

  public SystematicAllocationEventSampler(int samplingInterval) {
    if (samplingInterval < 1) {
      throw new IllegalArgumentException("Invalid sampling interval " + samplingInterval);
    }
    this.samplingInterval = samplingInterval;
    // set counter to sample first event
    this.counter = samplingInterval;
  }

  @Override
  public boolean shouldSample(RecordedEvent event) {
    counter++;
    if (counter < samplingInterval) {
      return false;
    }
    counter = 0;
    return true;
  }

  @Override
  public void addAttributes(
      BiConsumer<String, String> stringAttributeAdder,
      BiConsumer<String, Long> longAttributeAdder) {
    stringAttributeAdder.accept(SAMPLER_NAME_KEY, "Systematic sampler");
    longAttributeAdder.accept(SAMPLER_INTERVAL_KEY, (long) samplingInterval);
  }
}
