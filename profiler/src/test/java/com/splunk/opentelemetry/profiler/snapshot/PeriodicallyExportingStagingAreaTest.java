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

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PeriodicallyExportingStagingAreaTest {
  private final InMemoryStackTraceExporter exporter = new InMemoryStackTraceExporter();
  private final Duration emptyDuration = Duration.ofMillis(50);

  @Test
  void automaticallyExportStackTraces() {
    var stackTrace = Snapshotting.stackTrace().build();
    try (var stagingArea = new PeriodicallyExportingStagingArea(() -> exporter, emptyDuration, 10)) {
      stagingArea.stage(stackTrace);
      await().untilAsserted(() -> assertThat(exporter.stackTraces()).contains(stackTrace));
    }
  }

  @Test
  void continuallyExportingStackTracesPeriodically() {
    var stackTrace1 = Snapshotting.stackTrace().build();
    var stackTrace2 = Snapshotting.stackTrace().build();
    var stackTrace3 = Snapshotting.stackTrace().build();

    try (var stagingArea = new PeriodicallyExportingStagingArea(() -> exporter, emptyDuration, 10)) {
      stagingArea.stage(stackTrace1);
      await().untilAsserted(() -> assertThat(exporter.stackTraces()).contains(stackTrace1));

      stagingArea.stage(stackTrace2);
      stagingArea.stage(stackTrace3);
      await().untilAsserted(() -> assertThat(exporter.stackTraces()).contains(stackTrace2, stackTrace3));
    }
  }

  @Test
  void exportMultipleStackTracesToLogExporter() {
    var stackTrace1 = Snapshotting.stackTrace().build();
    var stackTrace2 = Snapshotting.stackTrace().build();

    try (var stagingArea = new PeriodicallyExportingStagingArea(() -> exporter, emptyDuration, 10)) {
      stagingArea.stage(stackTrace1);
      stagingArea.stage(stackTrace2);

      await().untilAsserted(() -> assertThat(exporter.stackTraces()).contains(stackTrace1, stackTrace2));
    }
  }

  @Test
  void stackTracesAreNotExportedMultipleTimes() {
    var stackTrace = Snapshotting.stackTrace().build();

    try (var stagingArea = new PeriodicallyExportingStagingArea(() -> exporter, emptyDuration, 10)) {
      stagingArea.stage(stackTrace);
      await().until(() -> !exporter.stackTraces().isEmpty());

      assertEquals(List.of(stackTrace), exporter.stackTraces());
    }
  }

  @Test
  void stackTracesAreExportedImmediatelyUponShutdown() {
    var stackTrace = Snapshotting.stackTrace().build();

    var stagingArea = new PeriodicallyExportingStagingArea(() -> exporter, Duration.ofDays(1), 10);
    stagingArea.stage(stackTrace);
    stagingArea.close();

    await().untilAsserted(() -> assertEquals(List.of(stackTrace), exporter.stackTraces()));
  }

  @Test
  void doNotAcceptStackTracesAfterShutdown() {
    var stackTrace = Snapshotting.stackTrace().build();

    var stagingArea = new PeriodicallyExportingStagingArea(() -> exporter, emptyDuration, 10);
    stagingArea.close();
    stagingArea.stage(stackTrace);

    assertEquals(Collections.emptyList(), exporter.stackTraces());
  }

  @Test
  void exportStackTracesWhenCapacityReached() {
    try (var stagingArea =
        new PeriodicallyExportingStagingArea(() -> exporter, Duration.ofDays(1), 2)) {
      stagingArea.stage(Snapshotting.stackTrace().build());
      stagingArea.stage(Snapshotting.stackTrace().build());

      await().untilAsserted(() -> assertEquals(2, exporter.stackTraces().size()));
    }
  }

  @Test
  void doNotExportStackTraceMultipleTimes() {
    var stackTrace1 = Snapshotting.stackTrace().build();
    var stackTrace2 = Snapshotting.stackTrace().build();

    var executor = Executors.newFixedThreadPool(2);
    var startLatch = new CountDownLatch(1);

    try (var stagingArea =
        new PeriodicallyExportingStagingArea(() -> exporter, Duration.ofDays(1), 1)) {
      executor.submit(stage(stagingArea, stackTrace1, startLatch));
      executor.submit(stage(stagingArea, stackTrace2, startLatch));
      startLatch.countDown();

      await().until(() -> !exporter.stackTraces().isEmpty());
      assertThat(exporter.stackTraces()).containsOnlyOnce(stackTrace1, stackTrace2);
    }
  }

  private Runnable stage(
      StagingArea stagingArea, StackTrace stackTrace, CountDownLatch startLatch) {
    return () -> {
      try {
        startLatch.await();
        stagingArea.stage(stackTrace);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    };
  }
}
