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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class PeriodicallyExportingStagingArea implements StagingArea {
  private static final String WORKER_THREAD_NAME =
      PeriodicallyExportingStagingArea.class.getSimpleName() + "_WorkerThread";

  private volatile boolean closed = false;

  private final Worker worker;

  PeriodicallyExportingStagingArea(
      Supplier<StackTraceExporter> exporter, Duration delay, int capacity) {
    worker = new Worker(exporter, delay, capacity);
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

    // Wait for the worker thread to exit. Note that this does not guarantee that the pending items
    // are exported as we don't attempt to wait for the actual export to complete.
    try {
      worker.join();
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
    private final Duration delay;
    private final int maxExportBatchSize;

    private volatile boolean shutdown = false;
    private long nextExportTime;

    private Worker(Supplier<StackTraceExporter> exporter, Duration delay, int maxExportBatchSize) {
      this.exporter = exporter;
      this.delay = delay;
      this.maxExportBatchSize = maxExportBatchSize;
      // set the queue size to 4x the batch size, in sdk batch processors both of these are
      // configurable but by default queue size is also 4*batch size
      this.queue = new ArrayBlockingQueue<>(maxExportBatchSize * 4);

      updateNextExportTime();
    }

    void add(StackTrace stackTrace) {
      // If queue is full drop the stack trace, not much we can do.
      queue.offer(stackTrace);
    }

    @Override
    public void run() {
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
      nextExportTime = System.nanoTime() + delay.toNanos();
    }

    private void shutdown() throws InterruptedException {
      shutdown = true;
      // we don't care if the queue is full and offer fails, we only wish to ensure that there is
      // something in the queue so that shutdown could start immediately
      queue.offer(SHUTDOWN_MARKER);
    }
  }
}
