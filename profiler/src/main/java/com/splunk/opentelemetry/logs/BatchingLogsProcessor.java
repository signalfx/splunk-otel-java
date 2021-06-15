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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * A log processor that batches logs until a max number are buffered or a time limit has passed,
 * which ever comes first.
 */
public class BatchingLogsProcessor implements LogsProcessor {

  private final int maxBatchSize;
  private final Duration maxTimeBetweenBatches;
  private final List<LogEntry> batch;
  private final Consumer<List<LogEntry>> batchAction;
  private final Object lock = new Object();
  private WatchdogTimer watchdog;

  public BatchingLogsProcessor(
      Duration maxTimeBetweenBatches, int maxBatchSize, Consumer<List<LogEntry>> batchAction) {
    this.maxTimeBetweenBatches = maxTimeBetweenBatches;
    this.maxBatchSize = maxBatchSize;
    this.batchAction = batchAction;
    this.batch = new ArrayList<>(maxBatchSize);
  }

  void start() {
    synchronized (lock) {
      if (watchdog != null) {
        throw new IllegalStateException("Already running");
      }
      watchdog = new WatchdogTimer(maxTimeBetweenBatches, this::doAction);
      watchdog.start();
    }
  }

  void stop() {
    synchronized (lock) {
      if (watchdog == null) {
        throw new IllegalStateException("Not running");
      }
      watchdog.stop();
      watchdog = null;
      doAction();
    }
  }

  @Override
  public void log(LogEntry log) {
    synchronized (lock) {
      batch.add(log);
      if (batch.size() >= maxBatchSize) {
        doAction();
      }
    }
  }

  private void doAction() {
    synchronized (lock) {
      if (batch.isEmpty()) {
        return;
      }
      batchAction.accept(Collections.unmodifiableList(batch));
      batch.clear();
    }
  }
}
