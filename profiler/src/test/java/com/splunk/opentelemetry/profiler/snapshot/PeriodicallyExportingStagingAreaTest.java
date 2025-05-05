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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PeriodicallyExportingStagingAreaTest {
  private final InMemoryStackTraceExporter exporter = new InMemoryStackTraceExporter();
  private final Duration emptyDuration = Duration.ofMillis(50);

  @Test
  void automaticallyExportStackTraces() {
    var stackTrace = Snapshotting.stackTrace().build();
    try (var stagingArea =
        new PeriodicallyExportingStagingArea(() -> exporter, emptyDuration, 10)) {
      stagingArea.stage(stackTrace);
      await().untilAsserted(() -> assertThat(exporter.stackTraces()).contains(stackTrace));
    }
  }

  @Test
  void continuallyExportingStackTracesPeriodically() {
    var stackTrace1 = Snapshotting.stackTrace().build();
    var stackTrace2 = Snapshotting.stackTrace().build();
    var stackTrace3 = Snapshotting.stackTrace().build();

    try (var stagingArea =
        new PeriodicallyExportingStagingArea(() -> exporter, emptyDuration, 10)) {
      stagingArea.stage(stackTrace1);
      await().untilAsserted(() -> assertThat(exporter.stackTraces()).contains(stackTrace1));

      stagingArea.stage(stackTrace2);
      stagingArea.stage(stackTrace3);
      await()
          .untilAsserted(
              () -> assertThat(exporter.stackTraces()).contains(stackTrace2, stackTrace3));
    }
  }

  @Test
  void exportMultipleStackTracesToLogExporter() {
    var stackTrace1 = Snapshotting.stackTrace().build();
    var stackTrace2 = Snapshotting.stackTrace().build();

    try (var stagingArea =
        new PeriodicallyExportingStagingArea(() -> exporter, emptyDuration, 10)) {
      stagingArea.stage(stackTrace1);
      stagingArea.stage(stackTrace2);

      await()
          .untilAsserted(
              () -> assertThat(exporter.stackTraces()).contains(stackTrace1, stackTrace2));
    }
  }

  @Test
  void stackTracesAreNotExportedMultipleTimes() {
    var stackTrace = Snapshotting.stackTrace().build();

    try (var stagingArea =
        new PeriodicallyExportingStagingArea(() -> exporter, emptyDuration, 10)) {
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
  void exportStackTracesOnNormalScheduleEvenAfterCapacityReached() {
    try (var stagingArea = new PeriodicallyExportingStagingArea(() -> exporter, emptyDuration, 2)) {
      stagingArea.stage(Snapshotting.stackTrace().build());
      stagingArea.stage(Snapshotting.stackTrace().build());
      stagingArea.stage(Snapshotting.stackTrace().build());

      await().untilAsserted(() -> assertEquals(3, exporter.stackTraces().size()));
    }
  }

  @Test
  void doNotExportStackTraceMultipleTimes() {
    var stackTrace1 = Snapshotting.stackTrace().build();
    var stackTrace2 = Snapshotting.stackTrace().build();

    var startLatch = new CountDownLatch(1);

    try (var executor = Executors.newFixedThreadPool(2);
        var stagingArea =
            new PeriodicallyExportingStagingArea(() -> exporter, Duration.ofDays(1), 1)) {
      executor.submit(stage(stagingArea, startLatch, stackTrace1));
      executor.submit(stage(stagingArea, startLatch, stackTrace2));
      startLatch.countDown();

      await().until(() -> !exporter.stackTraces().isEmpty());
      assertThat(exporter.stackTraces()).containsOnlyOnce(stackTrace1, stackTrace2);
    }
  }

  @Test
  void multipleThreadsStagingStackTracesWhenCapacityReachedDoesNotCauseMultipleExports() {
    var stackTrace1 = Snapshotting.stackTrace().build();
    var stackTrace2 = Snapshotting.stackTrace().build();
    var stackTrace3 = Snapshotting.stackTrace().build();
    var stackTrace4 = Snapshotting.stackTrace().build();

    var startLatch = new CountDownLatch(1);

    try (var executor = Executors.newFixedThreadPool(2);
        var exporter = new CallCountingStackTraceExporter();
        var stagingArea =
            new PeriodicallyExportingStagingArea(() -> exporter, Duration.ofDays(1), 3)) {
      executor.submit(stage(stagingArea, startLatch, stackTrace1, stackTrace2));
      executor.submit(stage(stagingArea, startLatch, stackTrace3, stackTrace4));
      startLatch.countDown();

      await().until(() -> !exporter.stackTraces().isEmpty());
      assertEquals(1, exporter.timesCalled.get());
    }
  }

  private Runnable stage(
      StagingArea stagingArea, CountDownLatch startLatch, StackTrace... stackTraces) {
    return () -> {
      try {
        startLatch.await();
        Arrays.stream(stackTraces).forEach(stagingArea::stage);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    };
  }

  private static class CallCountingStackTraceExporter extends InMemoryStackTraceExporter {
    private final AtomicInteger timesCalled = new AtomicInteger();

    @Override
    public void export(Collection<StackTrace> stackTraces) {
      timesCalled.incrementAndGet();
      super.export(stackTraces);
    }
  }
}
