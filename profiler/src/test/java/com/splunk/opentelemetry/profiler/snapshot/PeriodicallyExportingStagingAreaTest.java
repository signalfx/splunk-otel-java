package com.splunk.opentelemetry.profiler.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PeriodicallyExportingStagingAreaTest {
  private final InMemoryStackTraceExporter exporter = new InMemoryStackTraceExporter();
  private final Duration emptyDuration = Duration.ofMillis(5);
  private final PeriodicallyExportingStagingArea stagingArea = new PeriodicallyExportingStagingArea(() -> exporter, emptyDuration);

  @AfterEach
  void teardown() {
    stagingArea.close();
  }

  @Test
  void automaticallyEmptyStagingAreaPeriodically() {
    var stackTrace = Snapshotting.stackTrace().build();

    stagingArea.stage("", stackTrace);
    await().until(() -> !exporter.stackTraces().isEmpty());

    assertThat(exporter.stackTraces()).contains(stackTrace);
  }

  @Test
  void exportMultipleStackTracesToLogExporter() {
    var stackTrace1 = Snapshotting.stackTrace().withId(1).withName("one").build();
    var stackTrace2 = Snapshotting.stackTrace().withId(2).withName("two").build();

    stagingArea.stage("", stackTrace1);
    stagingArea.stage("", stackTrace2);
    await().until(() -> !exporter.stackTraces().isEmpty());

    assertThat(exporter.stackTraces()).contains(stackTrace1, stackTrace2);
  }

  @Test
  void stackTracesAreNotExportedMultipleTimes() {
    var stackTrace = Snapshotting.stackTrace().build();

    stagingArea.stage("", stackTrace);
    await().until(() -> !exporter.stackTraces().isEmpty());

    assertEquals(List.of(stackTrace), exporter.stackTraces());
  }
  
  @Test
  void stackTracesAreExportedImmediatelyUponShutdown() {
    var stackTrace = Snapshotting.stackTrace().build();

    var stagingArea = new PeriodicallyExportingStagingArea(() -> exporter, Duration.ofDays(1));
    stagingArea.stage("", stackTrace);
    stagingArea.close();

    assertEquals(List.of(stackTrace), exporter.stackTraces());
  }

  @Test
  void provideCopyOfStackTracesWhenExporting() {
    var stackTrace = Snapshotting.stackTrace().build();

    var exporter = new SimpleStackTraceExporter();
    var stagingArea = new PeriodicallyExportingStagingArea(() -> exporter, emptyDuration);
    stagingArea.stage("", stackTrace);
    stagingArea.close();

    assertEquals(List.of(stackTrace), exporter.stackTraces);
  }

  static class SimpleStackTraceExporter implements StackTraceExporter {
    private List<StackTrace> stackTraces;

    @Override
    public void export(List<StackTrace> stackTraces) {
      this.stackTraces = stackTraces;
    }
  }
}
