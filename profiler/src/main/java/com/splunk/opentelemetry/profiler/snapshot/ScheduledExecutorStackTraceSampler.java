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

import com.google.common.annotations.VisibleForTesting;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

class ScheduledExecutorStackTraceSampler implements StackTraceSampler {
  private static final Logger LOGGER =
      Logger.getLogger(ScheduledExecutorStackTraceSampler.class.getName());
  private static final int SCHEDULER_INITIAL_DELAY = 0;
  private static final Duration SCHEDULER_PERIOD = Duration.ofMillis(20);
  private static final int MAX_ENTRY_DEPTH = 200;

  private final ConcurrentMap<Long, ScheduledExecutorService> samplers = new ConcurrentHashMap<>();
  private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
  private final StagingArea stagingArea;
  private final Duration samplingPeriod;

  ScheduledExecutorStackTraceSampler(StagingArea stagingArea) {
    this(stagingArea, SCHEDULER_PERIOD);
  }

  @VisibleForTesting
  ScheduledExecutorStackTraceSampler(StagingArea stagingArea, Duration samplingPeriod) {
    this.stagingArea = stagingArea;
    this.samplingPeriod = samplingPeriod;
  }

  @Override
  public void startSampling(String traceId, long threadId) {
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    samplers.put(threadId, scheduler);
    scheduler.scheduleAtFixedRate(
        new StackTraceGatherer(threadId),
        SCHEDULER_INITIAL_DELAY,
        samplingPeriod.toMillis(),
        TimeUnit.MILLISECONDS);
  }

  @Override
  public void stopSampling(long threadId) {
    ScheduledExecutorService scheduler = samplers.remove(threadId);
    if (scheduler != null) {
      scheduler.shutdown();
    }
    stagingArea.empty(threadId);
  }

  class StackTraceGatherer implements Runnable {
    private final long threadId;

    StackTraceGatherer(long threadId) {
      this.threadId = threadId;
    }

    @Override
    public void run() {
      Instant now = Instant.now();
      try {
        ThreadInfo threadInfo = threadMXBean.getThreadInfo(threadId, MAX_ENTRY_DEPTH);
        StackTrace stackTrace = StackTrace.from(now, threadInfo);
        stagingArea.stage(threadId, stackTrace);
      } catch (Exception e) {
        LOGGER.severe(e::getMessage);
      }
    }
  }
}
