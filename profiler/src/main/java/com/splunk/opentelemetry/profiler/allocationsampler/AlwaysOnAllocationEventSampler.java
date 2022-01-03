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

package com.splunk.opentelemetry.profiler.allocationsampler;

import io.opentelemetry.api.common.AttributesBuilder;
import jdk.jfr.consumer.RecordedEvent;

class AlwaysOnAllocationEventSampler implements AllocationEventSampler {
  static AllocationEventSampler INSTANCE = new AlwaysOnAllocationEventSampler();

  @Override
  public boolean shouldSample(RecordedEvent event) {
    return true;
  }

  @Override
  public void addAttributes(AttributesBuilder builder) {
    // always on sampler does not add any attributes to log data
  }
}
