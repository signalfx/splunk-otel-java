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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

class PeriodicallyExportingStagingArea implements StagingArea {
  private volatile boolean closed = false;

  private final Exporter worker;

  PeriodicallyExportingStagingArea(
      Supplier<StackTraceExporter> exporter, Duration emptyDuration, int capacity) {
    worker = new Exporter(exporter, emptyDuration, capacity);
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

    try {
      worker.shutdown();
      worker.join(TimeUnit.SECONDS.toMillis(5));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static class Exporter extends Thread {
    private final ConcurrentLinkedQueue<StackTrace> queue = new ConcurrentLinkedQueue<>();
    private final DelayQueue<WakeUp> wakeUp = new DelayQueue<>();
    private final AtomicInteger staged = new AtomicInteger();
    private volatile boolean shutdown = false;

    private final Supplier<StackTraceExporter> exporter;
    private final Duration frequency;
    private final int capacity;

    private Exporter(Supplier<StackTraceExporter> exporter, Duration frequency, int capacity) {
      this.exporter = exporter;
      this.frequency = frequency;
      this.capacity = capacity;

      wakeUp.add(WakeUp.later(frequency));
    }

    void add(StackTrace stackTrace) {
      if (queue.offer(stackTrace)) {
        if (staged.incrementAndGet() >= capacity) {
          wakeUp.add(WakeUp.NOW);
        }
      }
    }

    @Override
    public void run() {
      while(keepRunning()) {
        try {
          WakeUp command = wakeUp.poll(frequency.toNanos(), TimeUnit.NANOSECONDS);
          exportStackTraces();
          scheduleNextExport(command);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }

    private void exportStackTraces() {
      List<StackTrace> stackTracesToExport = emptyStagingArea();
      exporter.get().export(stackTracesToExport);
      staged.addAndGet(-stackTracesToExport.size());
    }

    private List<StackTrace> emptyStagingArea() {
      List<StackTrace> stackTracesToExport = new ArrayList<>();
      Iterator<StackTrace> iterator = queue.iterator();
      while (iterator.hasNext()) {
        stackTracesToExport.add(iterator.next());
        iterator.remove();
      }
      return stackTracesToExport;
    }

    private void scheduleNextExport(WakeUp command) {
      if (command != null && command.wasScheduled()) {
        wakeUp.add(WakeUp.later(frequency));
      }
    }

    private boolean keepRunning() {
      return !queue.isEmpty() || (!shutdown && !isInterrupted());
    }

    private void shutdown() throws InterruptedException {
      shutdown = true;
      wakeUp.add(WakeUp.NOW);
    }
  }

  private static class WakeUp implements Delayed {
    private static final Comparator<WakeUp> TIME_COMPARATOR = Comparator.comparing(wakeup -> wakeup.time);
    private static final WakeUp NOW = new WakeUp(Duration.ZERO, false);

    private static WakeUp later(Duration delay) {
      return new WakeUp(delay, true);
    }

    private final long time;
    private final boolean scheduled;

    private WakeUp(Duration delay, boolean scheduled) {
      this.time = System.nanoTime() + delay.toNanos();
      this.scheduled = scheduled;
    }

    boolean wasScheduled() {
      return scheduled;
    }

    @Override
    public long getDelay(TimeUnit unit) {
      return unit.convert(time - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
      return TIME_COMPARATOR.compare(this, (WakeUp)other);
    }
  }
}
