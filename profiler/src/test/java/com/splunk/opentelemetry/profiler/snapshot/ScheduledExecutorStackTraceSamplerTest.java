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

package com.splunk.opentelemetry.profiler.snapshot;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ScheduledExecutorStackTraceSamplerTest {
  private static final Duration HALF_SECOND = Duration.ofMillis(500);
  private static final Duration PERIOD = Duration.ofMillis(20);

  private final InMemoryStagingArea staging = new InMemoryStagingArea();
  private final ScheduledExecutorStackTraceSampler sampler =
      new ScheduledExecutorStackTraceSampler(staging, PERIOD);

  @Test
  void takeStackTraceSampleForGivenThread() {
    sampler.startSampling("", Thread.currentThread().getId());
    await().atMost(HALF_SECOND).until(() -> !staging.allStackTraces().isEmpty());
    sampler.stopSampling(Thread.currentThread().getId());
  }

  @Test
  void continuallySampleThreadForStackTraces() {
    int expectedSamples = (int) HALF_SECOND.dividedBy(PERIOD.multipliedBy(2));

    sampler.startSampling("", Thread.currentThread().getId());
    await().atMost(HALF_SECOND).until(() -> staging.allStackTraces().size() >= expectedSamples);
    sampler.stopSampling(Thread.currentThread().getId());
  }

  @Test
  void emptyStagingAreaAfterSamplingStops() {
    int expectedSamples = (int) HALF_SECOND.dividedBy(PERIOD.multipliedBy(2));

    sampler.startSampling("", Thread.currentThread().getId());
    await().atMost(HALF_SECOND).until(() -> staging.allStackTraces().size() >= expectedSamples);
    sampler.stopSampling(Thread.currentThread().getId());

    assertEquals(Collections.emptyList(), staging.allStackTraces());
  }
}
