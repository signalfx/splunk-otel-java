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

package com.splunk.opentelemetry.logs;

import static java.util.Objects.requireNonNull;

import com.splunk.opentelemetry.profiler.util.HelpfulExecutors;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

/**
 * A log processor that batches logs until a max number are buffered or a time limit has passed,
 * which ever comes first.
 */
public class BatchingLogsProcessor implements LogsProcessor {

  private static final int DEFAULT_MAX_BATCH_SIZE = 250;
  private static final Duration DEFAULT_MAX_TIME_BETWEEN_BATCHES = Duration.ofSeconds(10);

  private final int maxBatchSize;
  private final Duration maxTimeBetweenBatches;
  private final List<LogEntry> batch;
  private final Consumer<List<LogEntry>> batchAction;
  private final ScheduledExecutorService executorService;
  private final Object lock = new Object();
  private WatchdogTimer watchdog;

  public BatchingLogsProcessor(Builder builder) {
    this.maxTimeBetweenBatches = builder.maxTimeBetweenBatches;
    this.maxBatchSize = builder.maxBatchSize;
    this.batchAction = builder.batchAction;
    this.executorService = builder.executorService;
    batch = new ArrayList<>(maxBatchSize);
  }

  public void start() {
    synchronized (lock) {
      if (watchdog != null) {
        throw new IllegalStateException("Already running");
      }
      watchdog = new WatchdogTimer(maxTimeBetweenBatches, this::doAction, executorService);
      watchdog.start();
    }
  }

  void stop() {
    synchronized (lock) {
      if (watchdog == null) {
        throw new IllegalStateException("Not running");
      }
      doAction();
      watchdog.stop();
      watchdog = null;
    }
  }

  @Override
  public void log(LogEntry log) {
    synchronized (lock) {
      batch.add(log);
      if (batch.size() >= maxBatchSize) {
        doAction();
        watchdog.reset();
      }
    }
  }

  private void doAction() {
    synchronized (lock) {
      if (batch.isEmpty()) {
        return;
      }
      List<LogEntry> batchCopy = new ArrayList<>(batch);
      executorService.submit(() -> batchAction.accept(batchCopy));
      batch.clear();
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private int maxBatchSize = DEFAULT_MAX_BATCH_SIZE;
    private Duration maxTimeBetweenBatches = DEFAULT_MAX_TIME_BETWEEN_BATCHES;
    private Consumer<List<LogEntry>> batchAction;
    private ScheduledExecutorService executorService;

    public Builder maxBatchSize(int maxBatchSize) {
      this.maxBatchSize = maxBatchSize;
      return this;
    }

    public Builder maxTimeBetweenBatches(Duration maxTimeBetweenBatches) {
      this.maxTimeBetweenBatches = maxTimeBetweenBatches;
      return this;
    }

    public Builder batchAction(Consumer<List<LogEntry>> batchAction) {
      this.batchAction = batchAction;
      return this;
    }

    public Builder executorService(ScheduledExecutorService executorService) {
      this.executorService = executorService;
      return this;
    }

    public BatchingLogsProcessor build() {
      if (executorService == null) {
        executorService =
            HelpfulExecutors.newSingleThreadedScheduledExecutor("BatchingLogsProcessor action");
      }
      requireNonNull(batchAction);
      return new BatchingLogsProcessor(this);
    }
  }
}
