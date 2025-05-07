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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StalledTraceDetectingTraceRegistryTest {
  private static final Duration STALLED_TIME_LIMIT = Duration.ofMillis(10);

  private final TraceRegistry delegate = new SimpleTraceRegistry();
  private final ObservableStackTraceSampler sampler = new ObservableStackTraceSampler();
  private final StalledTraceDetectingTraceRegistry registry =
      new StalledTraceDetectingTraceRegistry(delegate, () -> sampler, STALLED_TIME_LIMIT);

  @AfterEach
  void teardown() throws Exception {
    registry.close();
  }

  @Test
  void delegateTraceRegistration() {
    var spanContext = Snapshotting.spanContext().build();
    registry.register(spanContext);
    assertThat(delegate.isRegistered(spanContext)).isTrue();
  }

  @Test
  void delegateTraceDeregistration() {
    var spanContext = Snapshotting.spanContext().build();

    registry.register(spanContext);
    registry.unregister(spanContext);

    assertThat(delegate.isRegistered(spanContext)).isFalse();
  }

  @Test
  void delegateTraceRegistrationLookup() {
    var spanContext = Snapshotting.spanContext().build();
    delegate.register(spanContext);
    assertThat(registry.isRegistered(spanContext)).isTrue();
  }

  @Test
  void removeRegisteredTracesAfterStalledTimeLimitPasses() {
    var spanContext = Snapshotting.spanContext().build();

    registry.register(spanContext);
    assertThat(registry.isRegistered(spanContext)).isTrue();

    await().untilAsserted(() -> assertThat(delegate.isRegistered(spanContext)).isFalse());
  }

  @Test
  void continueRemovingRegisteredTracesAfterStalledTimeLimitPasses() {
    var spanContext = Snapshotting.spanContext().build();

    registry.register(spanContext);
    assertThat(registry.isRegistered(spanContext)).isTrue();
    await().untilAsserted(() -> assertThat(delegate.isRegistered(spanContext)).isFalse());

    registry.register(spanContext);
    assertThat(registry.isRegistered(spanContext)).isTrue();
    await().untilAsserted(() -> assertThat(delegate.isRegistered(spanContext)).isFalse());
  }

  @Test
  void stopTraceSamplingWhenStalledTraceUnregistered() {
    var spanContext = Snapshotting.spanContext().build();

    registry.register(spanContext);
    sampler.start(spanContext);

    await().untilAsserted(() -> assertThat(sampler.isBeingSampled(spanContext)).isFalse());
  }
}
