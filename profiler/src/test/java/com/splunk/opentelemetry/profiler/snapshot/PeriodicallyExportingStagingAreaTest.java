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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PeriodicallyExportingStagingAreaTest {
  private final InMemoryStackTraceExporter exporter = new InMemoryStackTraceExporter();
  private final Duration emptyDuration = Duration.ofMillis(5);
  private final PeriodicallyExportingStagingArea stagingArea =
      new PeriodicallyExportingStagingArea(() -> exporter, emptyDuration, 10);

  @AfterEach
  void teardown() {
    stagingArea.close();
  }

  @Test
  void automaticallyEmptyStagingAreaPeriodically() {
    var stackTrace = Snapshotting.stackTrace().build();

    stagingArea.stage(stackTrace);
    await().until(() -> !exporter.stackTraces().isEmpty());

    assertThat(exporter.stackTraces()).contains(stackTrace);
  }

  @Test
  void exportMultipleStackTracesToLogExporter() {
    var stackTrace1 = Snapshotting.stackTrace().withId(1).withName("one").build();
    var stackTrace2 = Snapshotting.stackTrace().withId(2).withName("two").build();

    stagingArea.stage(stackTrace1);
    stagingArea.stage(stackTrace2);
    await().until(() -> !exporter.stackTraces().isEmpty());

    assertThat(exporter.stackTraces()).contains(stackTrace1, stackTrace2);
  }

  @Test
  void stackTracesAreNotExportedMultipleTimes() {
    var stackTrace = Snapshotting.stackTrace().build();

    stagingArea.stage(stackTrace);
    await().until(() -> !exporter.stackTraces().isEmpty());

    assertEquals(List.of(stackTrace), exporter.stackTraces());
  }

  @Test
  void provideCopyOfStackTracesWhenExporting() {
    var stackTrace = Snapshotting.stackTrace().build();

    var exporter = new SimpleStackTraceExporter();
    var stagingArea = new PeriodicallyExportingStagingArea(() -> exporter, emptyDuration, 10);
    stagingArea.stage(stackTrace);
    stagingArea.close();

    assertEquals(List.of(stackTrace), exporter.stackTraces);
  }

  @Test
  void stackTracesAreExportedImmediatelyUponShutdown() {
    var stackTrace = Snapshotting.stackTrace().build();

    var stagingArea = new PeriodicallyExportingStagingArea(() -> exporter, Duration.ofDays(1), 10);
    stagingArea.stage(stackTrace);
    stagingArea.close();

    assertEquals(List.of(stackTrace), exporter.stackTraces());
  }

  @Test
  void doNotAcceptStackTracesAfterShutdown() {
    var stackTrace = Snapshotting.stackTrace().build();

    stagingArea.close();
    stagingArea.stage(stackTrace);
    stagingArea.empty();

    assertEquals(Collections.emptyList(), exporter.stackTraces());
  }

  @Test
  void exportStackTracesWhenCapacityReached() {
    try (var stagingArea =
        new PeriodicallyExportingStagingArea(() -> exporter, Duration.ofDays(1), 2)) {
      stagingArea.stage(Snapshotting.stackTrace().build());
      stagingArea.stage(Snapshotting.stackTrace().build());
      assertEquals(2, exporter.stackTraces().size());
    }
  }

  static class SimpleStackTraceExporter implements StackTraceExporter {
    private Collection<StackTrace> stackTraces;

    @Override
    public void export(Collection<StackTrace> stackTraces) {
      this.stackTraces = stackTraces;
    }
  }
}
