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

package com.splunk.hackity.hack.control;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class BigDumper {

  private static final java.util.logging.Logger LOGGER =
      java.util.logging.Logger.getLogger(BigDumper.class.getName());

  private final ThreadMXBean threadMXBean;
  private final Consumer<ThreadInfo[]> threadDumpExporter;
  private final Object lock = new Object();
  private ScheduledExecutorService executorService = null;

  public BigDumper(Consumer<ThreadInfo[]> threadDumpExporter) {
    this.threadMXBean = ManagementFactory.getThreadMXBean();
    this.threadDumpExporter = threadDumpExporter;
  }

  public boolean startPeriodicDumper(int count, Duration interval) {
    synchronized(lock) {
      if(executorService != null){
        LOGGER.info("Periodic thread dumping is already started. Skipping request.");
        return false; //already running
      }
      if(count == 1){
        dump();
        return true;
      }
      executorService = Executors.newSingleThreadScheduledExecutor();

      LOGGER.info("Starting periodic thread dumps: count = " + count + " interval = " + interval);

      AtomicInteger counter = new AtomicInteger(count);
      executorService.scheduleWithFixedDelay(() -> {
        dump();
        if(counter.decrementAndGet() == 0){
          LOGGER.fine("Periodic thread dumping complete.");
          synchronized(lock) {
            executorService.shutdown();
            executorService = null;
          }
        }

      }, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
      return true; // started ok
    }
  }

  public void dump() {
    LOGGER.fine("Taking a thread dump");
    ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
    threadDumpExporter.accept(threadInfos);
  }
}
