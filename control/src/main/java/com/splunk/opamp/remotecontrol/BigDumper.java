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

package com.splunk.opamp.remotecontrol;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.logging.Level;

public class BigDumper {

  private static final java.util.logging.Logger logger =
      java.util.logging.Logger.getLogger(BigDumper.class.getName());

  private final ThreadMXBean threadMXBean;
  private final BiConsumer<String, ThreadInfo[]> threadDumpExporter;
  private final Object lock = new Object();
  private ScheduledExecutorService executorService = null;
  private boolean dumping;

  public BigDumper(BiConsumer<String, ThreadInfo[]> threadDumpExporter) {
    this.threadMXBean = ManagementFactory.getThreadMXBean();
    this.threadDumpExporter = threadDumpExporter;
  }

  public boolean startPeriodicDumper(String jobId, int count, Duration interval) {
    if (count < 1) {
      logger.warning("Thread dump count must be positive.");
      return false;
    }
    if (interval.isZero() || interval.isNegative()) {
      logger.warning("Thread dump interval must be positive.");
      return false;
    }

    synchronized (lock) {
      if (dumping) {
        logger.info("Periodic thread dumping is already started. Skipping request.");
        return false;
      }
      dumping = true;
      if (count > 1) {
        executorService = Executors.newSingleThreadScheduledExecutor();
      }
    }

    if (count == 1) {
      try {
        dump(jobId);
        return true;
      } finally {
        finishDumping();
      }
    }

    synchronized (lock) {
      logger.info("Starting periodic thread dumps: count = " + count + " interval = " + interval);

      AtomicInteger counter = new AtomicInteger(count);
      executorService.scheduleWithFixedDelay(
          () -> {
            try {
              dump(jobId);
              if (counter.decrementAndGet() == 0) {
                logger.fine("Periodic thread dumping complete.");
                finishDumping();
              }
            } catch (RuntimeException exception) {
              logger.log(Level.WARNING, "Periodic thread dumping failed.", exception);
              finishDumping();
            }
          },
          0,
          interval.toMillis(),
          TimeUnit.MILLISECONDS);
      return true;
    }
  }

  private void finishDumping() {
    synchronized (lock) {
      if (executorService != null) {
        executorService.shutdown();
        executorService = null;
      }
      dumping = false;
    }
  }

  public void dump(String jobId) {
    logger.fine("Taking a thread dump");
    ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
    threadDumpExporter.accept(jobId, threadInfos);
  }
}
