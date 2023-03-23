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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RateLimitingAllocationEventSamplerTest {

  @Test
  void parseRateLimit() {
    assertEquals(new RateLimitingAllocationEventSampler("100/s").maxEventsPerSecond(), 100);
    assertEquals(new RateLimitingAllocationEventSampler("100/m").maxEventsPerSecond(), 100.0 / 60);

    Assertions.assertThrowsExactly(
        IllegalArgumentException.class, () -> new RateLimitingAllocationEventSampler("100"));
    Assertions.assertThrowsExactly(
        IllegalArgumentException.class, () -> new RateLimitingAllocationEventSampler("100/h"));
    Assertions.assertThrowsExactly(
        IllegalArgumentException.class, () -> new RateLimitingAllocationEventSampler("1 /s"));
  }
}
