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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class PeriodicallyExportingStagingArea implements StagingArea {
  private static final String WORKER_THREAD_NAME =
      PeriodicallyExportingStagingArea.class.getSimpleName() + "_WorkerThread";

  private volatile boolean closed = false;

  private final Worker worker;

  PeriodicallyExportingStagingArea(
      Supplier<StackTraceExporter> exporter, Duration emptyDuration, int capacity) {
    // set the queue size to 4x the batch size, in sdk batch processors both of these are
    // configurable but by default queue size is also 4*batch size
    worker = new Worker(exporter, emptyDuration, capacity * 4, capacity);
    worker.setName(WORKER_THREAD_NAME);
    worker.setDaemon(true);
    worker.start();
  }

  @Override
  public void stage(StackTrace stackTrace) {
    if (closed) {
      return;
    }
    worker.add(stackTrace);
  }

  @Override
  public void empty() {}

  @Override
  public void close() {
    this.closed = true;

    worker.shutdown();
    // Wait for the worker thread to exit. Note that this does not guarantee that the pending items
    // are exported as we don't attempt to wait for the actual export to complete.
    try {
      worker.join(TimeUnit.SECONDS.toMillis(5));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }
  }

  private static class Worker extends Thread {
    // when shutting down we queue a fake stack trace to ensure that shutdown process starts
    // immediately
    private static final Object SHUTDOWN_MARKER = new Object();

    private final BlockingQueue<Object> queue;
    private final Supplier<StackTraceExporter> exporter;
    private final long scheduleDelayNanos;
    private final int maxExportBatchSize;

    private volatile boolean shutdown = false;
    private long nextExportTime;

    private Worker(
        Supplier<StackTraceExporter> exporter,
        Duration frequency,
        int maxQueueSize,
        int maxExportBatchSize) {
      this.exporter = exporter;
      this.scheduleDelayNanos = frequency.toNanos();
      this.queue = new ArrayBlockingQueue<>(maxQueueSize);
      this.maxExportBatchSize = maxExportBatchSize;
    }

    void add(StackTrace stackTrace) {
      if (!queue.offer(stackTrace)) {
        // queue is full, drop the stack trace
      }
    }

    @Override
    public void run() {
      updateNextExportTime();

      List<StackTrace> stackTracesToExport = new ArrayList<>();
      try {
        // run until shutdown is called and all queued spans are passed to the exporter
        while (!shutdown || !queue.isEmpty() || !stackTracesToExport.isEmpty()) {
          Object stackTrace = queue.poll(nextExportTime - System.nanoTime(), TimeUnit.NANOSECONDS);
          if (stackTrace != null && stackTrace != SHUTDOWN_MARKER) {
            stackTracesToExport.add((StackTrace) stackTrace);
          }
          // trigger export when either next export time is reached, we have max batch size, or we
          // are shutting down and have read all the queued stacks
          if (System.nanoTime() >= nextExportTime
              || stackTracesToExport.size() >= maxExportBatchSize
              || (shutdown && queue.isEmpty())) {
            exporter.get().export(stackTracesToExport);
            stackTracesToExport = new ArrayList<>();
            updateNextExportTime();
          }
        }
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
      }
    }

    private void updateNextExportTime() {
      nextExportTime = System.nanoTime() + scheduleDelayNanos;
    }

    private void shutdown() {
      shutdown = true;
      // we don't care if the queue is full and offer fails, we only wish to ensure that there is
      // something in the queue so that shutdown could start immediately
      queue.offer(SHUTDOWN_MARKER);
    }
  }
}
