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
import io.opentelemetry.api.trace.SpanContext;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
  private final Supplier<StagingArea> stagingArea;
  private final Supplier<SpanTracker> spanTracker;
  private final Duration samplingPeriod;
  private volatile boolean closed = false;

  ScheduledExecutorStackTraceSampler(
      Supplier<StagingArea> stagingArea,
      Supplier<SpanTracker> spanTracker,
      Duration samplingPeriod) {
    this.stagingArea = stagingArea;
    this.spanTracker = spanTracker;
    this.samplingPeriod = samplingPeriod;
  }

  @Override
  public void start(SpanContext spanContext) {
    if (closed) {
      return;
    }

    samplers.computeIfAbsent(
        spanContext.getTraceId(), id -> new ThreadSampler(spanContext, samplingPeriod));
  }

  @Override
  public void stop(SpanContext spanContext) {
    samplers.computeIfPresent(
        spanContext.getTraceId(),
        (traceId, sampler) -> {
          if (spanContext.equals(sampler.getSpanContext())) {
            sampler.shutdown();
            waitForShutdown(sampler);
            stagingArea.get().empty(spanContext.getTraceId());
            return null;
          }
          return sampler;
        });
  }

  @Override
  public void close() {
    closed = true;

    samplers.values().forEach(ThreadSampler::shutdown);
    samplers.values().forEach(this::waitForShutdown);
    samplers.clear();
  }

  private void waitForShutdown(ThreadSampler sampler) {
    try {
      if (!sampler.awaitTermination(samplingPeriod.multipliedBy(2))) {
        sampler.shutdownNow();
      }
    } catch (InterruptedException e) {
      sampler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private class ThreadSampler {
    private final ScheduledExecutorService scheduler;
    private final SpanContext spanContext;
    private final StackTraceGatherer gatherer;

    ThreadSampler(SpanContext spanContext, Duration samplingPeriod) {
      this.spanContext = spanContext;
      gatherer =
          new StackTraceGatherer(
              spanContext.getTraceId(), Thread.currentThread(), System.nanoTime());
      scheduler =
          HelpfulExecutors.newSingleThreadedScheduledExecutor(
              "stack-trace-sampler-" + spanContext.getTraceId());
      scheduler.scheduleAtFixedRate(
          gatherer, SCHEDULER_INITIAL_DELAY, samplingPeriod.toMillis(), TimeUnit.MILLISECONDS);
    }

    void shutdown() {
      scheduler.shutdown();
      gatherer.run();
    }

    void shutdownNow() {
      scheduler.shutdownNow();
    }

    boolean awaitTermination(Duration timeout) throws InterruptedException {
      return scheduler.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    SpanContext getSpanContext() {
      return spanContext;
    }
  }

  private class StackTraceGatherer implements Runnable {
    private final String traceId;
    private final Thread thread;
    private volatile long timestampNanos;

    StackTraceGatherer(String traceId, Thread thread, long timestampNanos) {
      this.traceId = traceId;
      this.thread = thread;
      this.timestampNanos = timestampNanos;
    }

    @Override
    public void run() {
      long previousTimestampNanos = timestampNanos;
      long currentSampleTimestamp = System.nanoTime();
      try {
        ThreadInfo threadInfo = threadMXBean.getThreadInfo(thread.getId(), Integer.MAX_VALUE);
        Duration samplingPeriod = Duration.ofNanos(currentSampleTimestamp - previousTimestampNanos);
        String spanId = retrieveActiveSpan(thread).getSpanId();
        StackTrace stackTrace =
            StackTrace.from(Instant.now(), samplingPeriod, threadInfo, traceId, spanId);
        stagingArea.get().stage(traceId, stackTrace);
      } catch (Exception e) {
        logger.log(Level.SEVERE, e, samplerErrorMessage(traceId, thread.getId()));
      } finally {
        timestampNanos = currentSampleTimestamp;
      }
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
