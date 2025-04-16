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

import io.opentelemetry.api.trace.SpanContext;
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
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

class ScheduledExecutorStackTraceSampler implements StackTraceSampler {
  private static final Logger logger =
      Logger.getLogger(ScheduledExecutorStackTraceSampler.class.getName());
  private static final int SCHEDULER_INITIAL_DELAY = 0;

  private final ConcurrentMap<String, ThreadSampler> samplers = new ConcurrentHashMap<>();
  private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
  private final StagingArea stagingArea;
  private final Supplier<SpanTracker> spanTracker;
  private final Duration samplingPeriod;

  ScheduledExecutorStackTraceSampler(
      StagingArea stagingArea, Supplier<SpanTracker> spanTracker, Duration samplingPeriod) {
    this.stagingArea = stagingArea;
    this.spanTracker = spanTracker;
    this.samplingPeriod = samplingPeriod;
  }

  @Override
  public void start(SpanContext spanContext) {
    samplers.computeIfAbsent(
        spanContext.getTraceId(),
        traceId -> new ThreadSampler(traceId, Thread.currentThread(), samplingPeriod));
  }

  @Override
  public void stop(SpanContext spanContext) {
    String traceId = spanContext.getTraceId();
    ThreadSampler scheduler = samplers.remove(traceId);
    if (scheduler != null) {
      scheduler.stop();
    }

    stagingArea.empty(spanContext.getTraceId());
  }

  private class ThreadSampler {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final StackTraceGatherer gatherer;

    private ThreadSampler(String traceId, Thread thread, Duration period) {
      gatherer = new StackTraceGatherer(traceId, thread, System.nanoTime());
      scheduler.scheduleAtFixedRate(
          gatherer, SCHEDULER_INITIAL_DELAY, period.toMillis(), TimeUnit.MILLISECONDS);
    }

    void stop() {
      scheduler.shutdown();
      gatherer.run();
    }
  }

  private class StackTraceGatherer implements Runnable {
    private final String traceId;
    private final Thread thread;
    private long timestampNanos;

    StackTraceGatherer(String traceId, Thread thread, long timestampNanos) {
      this.traceId = traceId;
      this.thread = thread;
      this.timestampNanos = timestampNanos;
    }

    @Override
    public void run() {
      long currentSampleTimestamp = System.nanoTime();
      try {
        ThreadInfo threadInfo = threadMXBean.getThreadInfo(thread.getId(), Integer.MAX_VALUE);
        Duration samplingPeriod = samplingPeriod(timestampNanos, currentSampleTimestamp);
        String spanId = retrieveActiveSpan(thread).getSpanId();
        StackTrace stackTrace =
            StackTrace.from(Instant.now(), samplingPeriod, threadInfo, traceId, spanId);
        stagingArea.stage(traceId, stackTrace);
      } catch (Exception e) {
        logger.log(Level.SEVERE, e, samplerErrorMessage(traceId, thread.getId()));
      } finally {
        timestampNanos = currentSampleTimestamp;
      }
    }

    private Duration samplingPeriod(long fromNanos, long toNanos) {
      return Duration.ofNanos(toNanos - fromNanos);
    }

    private SpanContext retrieveActiveSpan(Thread thread) {
      return spanTracker.get().getActiveSpan(thread).orElse(SpanContext.getInvalid());
    }

    private Supplier<String> samplerErrorMessage(String traceId, long threadId) {
      return () ->
          "Exception thrown attempting to stage callstacks for trace ID ' "
              + traceId
              + "' on profiled thread "
              + threadId;
    }
  }
}
