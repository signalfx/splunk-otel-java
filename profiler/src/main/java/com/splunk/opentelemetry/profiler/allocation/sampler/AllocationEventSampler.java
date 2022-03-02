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

/** A sampler that decides whether given allocation event should be sampled or discarded. */
public interface AllocationEventSampler {

  /**
   * @param event allocation event (jdk.ObjectAllocationInNewTLAB or
   *     jdk.ObjectAllocationOutsideTLAB)
   * @return true when given event is sampled
   */
  boolean shouldSample(RecordedEvent event);

  /**
   * Add attributes describing the sampling strategy to data.
   *
   * @param attributeAdder operation for adding attributes to data
   */
  void addAttributes(BiConsumer<String, String> attributeAdder);
}
