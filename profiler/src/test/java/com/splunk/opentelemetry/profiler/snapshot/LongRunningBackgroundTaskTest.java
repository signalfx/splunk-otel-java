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

import com.splunk.opentelemetry.profiler.snapshot.simulation.Background;
import com.splunk.opentelemetry.profiler.snapshot.simulation.Message;
import com.splunk.opentelemetry.profiler.snapshot.simulation.Server;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkExtension;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class LongRunningBackgroundTaskTest {
  private final InMemoryStagingArea staging = new InMemoryStagingArea();
  private final SnapshotProfilingSdkCustomizer customizer =
      Snapshotting.customizer().withRealStackTraceSampler().with(staging).build();

  @RegisterExtension
  public final OpenTelemetrySdkExtension sdk =
      OpenTelemetrySdkExtension.configure()
          .withProperty("splunk.snapshot.profiler.enabled", "true")
          .with(customizer)
          .with(new SnapshotVolumePropagator((c) -> true))
          .build();

  private CountDownLatch slowTaskLatch = new CountDownLatch(1);

  @RegisterExtension
  public final Server server =
      Server.builder(sdk).named("server").performing(Background.task(slowTask())).build();

  private Runnable slowTask() {
    return () -> {
      try {
        slowTaskLatch.await();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    };
  }

  @AfterEach
  void reset() {
    slowTaskLatch.countDown();
    slowTaskLatch = new CountDownLatch(1);
  }

  @Test
  void traceBackgroundThreadProfilingContinuesAfterEntrySpanEnds() {
    server.send(new Message());

    await().atMost(Duration.ofSeconds(2)).until(() -> server.waitForResponse() != null);
    staging.empty();
    await().until(() -> staging.allStackTraces().size() > 1);

    var profiledThreads =
        staging.allStackTraces().stream().mapToLong(StackTrace::getThreadId).distinct().count();

    // Expect the background thread to keep running after the server responds and for the
    // trace profiler to continue profiling
    assertEquals(1, profiledThreads);
  }
}
