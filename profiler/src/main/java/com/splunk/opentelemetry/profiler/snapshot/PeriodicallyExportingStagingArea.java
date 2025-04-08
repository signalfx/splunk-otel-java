package com.splunk.opentelemetry.profiler.snapshot;

import java.io.Closeable;
import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class PeriodicallyExportingStagingArea implements StagingArea, Closeable {
  private final Queue<StackTrace> stackTraces = new ConcurrentLinkedQueue<>();
  private final ScheduledExecutorService scheduler =  Executors.newSingleThreadScheduledExecutor();
  private final Supplier<StackTraceExporter> exporter;

  PeriodicallyExportingStagingArea(Supplier<StackTraceExporter> exporter, Duration emptyDuration) {
    this.exporter = exporter;
    scheduler.scheduleAtFixedRate(this::empty, 0, emptyDuration.toMillis(), TimeUnit.MILLISECONDS);
  }

  @Override
  public void stage(String traceId, StackTrace stackTrace) {
    stackTraces.add(stackTrace);
  }

  @Override
  public void empty(String traceId) {}

  private void empty() {
    exporter.get().export(stackTraces);
    stackTraces.clear();
  }

  @Override
  public void close() {
    scheduler.shutdown();
    empty();
  }
}
