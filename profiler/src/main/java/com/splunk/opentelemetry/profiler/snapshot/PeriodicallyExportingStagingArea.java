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

import com.splunk.opentelemetry.profiler.util.HelpfulExecutors;
import java.io.Closeable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class PeriodicallyExportingStagingArea implements StagingArea, Closeable {
  private final ScheduledExecutorService scheduler =
      HelpfulExecutors.newSingleThreadedScheduledExecutor("periodically-exporting-staging-area");
  private final Queue<StackTrace> stackTraces = new ConcurrentLinkedQueue<>();
  private final Supplier<StackTraceExporter> exporter;
  private volatile boolean closed = false;

  PeriodicallyExportingStagingArea(Supplier<StackTraceExporter> exporter, Duration emptyDuration) {
    this.exporter = exporter;
    scheduler.scheduleAtFixedRate(
        this::empty, emptyDuration.toMillis(), emptyDuration.toMillis(), TimeUnit.MILLISECONDS);
  }

  @Override
  public void stage(StackTrace stackTrace) {
    if (closed) {
      return;
    }
    stackTraces.add(stackTrace);
  }

  @Override
  public void empty() {
    if (stackTraces.isEmpty()) {
      return;
    }

    List<StackTrace> stackTracesToExport = new ArrayList<>(stackTraces);
    exporter.get().export(stackTracesToExport);
    stackTraces.removeAll(stackTracesToExport);
  }

  @Override
  public void close() {
    this.closed = true;

    stopScheduledExecutor();
    empty();
  }

  private void stopScheduledExecutor() {
    try {
      scheduler.shutdown();
      if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
