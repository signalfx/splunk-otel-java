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

  private final ConcurrentMap<String, StackSamplerExecution> samplers = new ConcurrentHashMap<>();
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
        traceId -> {
          ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
          StackTraceGatherer stackTraceGatherer =
              new StackTraceGatherer(
                  samplingPeriod, spanContext.getTraceId(), Thread.currentThread());
          scheduler.scheduleAtFixedRate(
              stackTraceGatherer,
              SCHEDULER_INITIAL_DELAY,
              samplingPeriod.toMillis(),
              TimeUnit.MILLISECONDS);
          return new StackSamplerExecution(scheduler, stackTraceGatherer);
        });
  }

  @Override
  public void stop(SpanContext spanContext) {
    StackSamplerExecution samplerExecution = samplers.remove(spanContext.getTraceId());
    samplerExecution.finish();
  }

  class StackTraceGatherer implements Runnable {
    private final Duration samplingPeriod;
    private final String traceId;
    private final Thread thread;

    StackTraceGatherer(Duration samplingPeriod, String traceId, Thread thread) {
      this.samplingPeriod = samplingPeriod;
      this.traceId = traceId;
      this.thread = thread;
    }

    @Override
    public void run() {
      try {
        Instant now = Instant.now();
        ThreadInfo threadInfo = threadMXBean.getThreadInfo(thread.getId(), Integer.MAX_VALUE);
        SpanContext spanContext = retrieveActiveSpan(thread);
        StackTrace stackTrace =
            StackTrace.from(now, samplingPeriod, threadInfo, traceId, spanContext.getSpanId());
        stagingArea.stage(traceId, stackTrace);
      } catch (Exception e) {
        logger.log(Level.SEVERE, e, samplerErrorMessage(traceId, thread.getId()));
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

    public void finish() {
      stagingArea.empty(traceId);
    }
  }

  private class StackSamplerExecution {
    private final StackTraceGatherer gatherer;
    private final ScheduledExecutorService executor;

    public StackSamplerExecution(ScheduledExecutorService executor, StackTraceGatherer gatherer) {
      this.executor = executor;
      this.gatherer = gatherer;
    }

    public void finish() {
      // take final sample so stack trace timing matches span
      gatherer.run();
      gatherer.finish();
      if (executor != null) {
        executor.shutdown();
      }
    }
  }
}
