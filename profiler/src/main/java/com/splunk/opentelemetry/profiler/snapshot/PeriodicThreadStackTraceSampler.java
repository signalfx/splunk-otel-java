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
import io.opentelemetry.api.trace.SpanContext;

import java.lang.management.ThreadInfo;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

class PeriodicThreadStackTraceSampler implements StackTraceSampler {
  private static final Logger logger =
      Logger.getLogger(PeriodicThreadStackTraceSampler.class.getName());

  private final ThreadSampler sampler;

  public PeriodicThreadStackTraceSampler(
      Supplier<StagingArea> staging, Supplier<SpanTracker> spanTracker, Duration samplingPeriod) {
    this(staging, spanTracker, new ThreadInfoCollector(), samplingPeriod);
  }

  @VisibleForTesting
  PeriodicThreadStackTraceSampler(
      Supplier<StagingArea> staging,
      Supplier<SpanTracker> spanTracker,
      ThreadInfoCollector collector,
      Duration samplingPeriod) {
    sampler = new ThreadSampler(staging, spanTracker, collector, samplingPeriod);
    sampler.setName("periodic-stack-trace-sampler");
    sampler.setDaemon(true);
    sampler.start();
  }

  @Override
  public void start(SpanContext spanContext) {
    sampler.add(Thread.currentThread(), spanContext.getTraceId(), spanContext.getSpanId());
  }

  @Override
  public void stop(String traceId, String spanId) {
    sampler.remove(traceId, spanId);
  }

  @Override
  public void close() {
    sampler.shutdown();
  }

  private static class ThreadSampler extends Thread {
    private static final Object SHUTDOWN_SIGNAL = new Object();

    private final Map<String, SamplingContext> traceSamplingContexts = new ConcurrentHashMap<>();
    private final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
    private final Supplier<StagingArea> staging;
    private final Supplier<SpanTracker> spanTracker;
    private final ThreadInfoCollector collector;
    private final Duration delay;

    private ThreadSampler(
        Supplier<StagingArea> staging,
        Supplier<SpanTracker> spanTracker,
        ThreadInfoCollector collector,
        Duration delay) {
      this.staging = staging;
      this.spanTracker = spanTracker;
      this.collector = collector;
      this.delay = delay;
    }

    void add(Thread thread, String traceId, String spanId) {
      traceSamplingContexts.computeIfAbsent(
          traceId,
          tid -> {
            SamplingContext context = new SamplingContext(thread, tid, spanId, System.nanoTime());
            sample(context);
            return context;
          });
    }

    void remove(String traceId, String spanId) {
      traceSamplingContexts.computeIfPresent(
          traceId,
          (t, context) -> {
            if (spanId.equals(context.spanId)) {
              sample(context);
              return null;
            }
            return context;
          });
    }

    private void sample(SamplingContext context) {
      long currentTimestamp = System.nanoTime();
      ThreadInfo threadInfo = collector.getThreadInfo(context.thread.getId());
      StackTrace stackTrace = toStackTrace(threadInfo, context, currentTimestamp);
      staging.get().stage(stackTrace);
      context.updateTimestamp(currentTimestamp);
    }

    void shutdown() {
      queue.add(SHUTDOWN_SIGNAL);
    }

    @Override
    public void run() {
      long nextSampleTime = System.nanoTime() + delay.toNanos();
      try {
        while (!Thread.currentThread().isInterrupted()) {
          Object object = queue.poll(nextSampleTime - System.nanoTime(), TimeUnit.NANOSECONDS);
          if (object == SHUTDOWN_SIGNAL) {
            return;
          }
          sample(traceSamplingContexts.values());
          nextSampleTime = System.nanoTime() + delay.toNanos();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    private void sample(Collection<SamplingContext> contexts) {
      if (contexts.isEmpty()) {
        return;
      }

      Map<Long, SamplingContext> threadContexts =
          contexts.stream().collect(Collectors.toMap(c -> c.thread.getId(), context -> context));
      long currentTimestamp = System.nanoTime();
      try {
        ThreadInfo[] threadInfos = collector.getThreadInfo(threadContexts.keySet());
        List<StackTrace> stackTraces = toStackTraces(threadInfos, threadContexts, currentTimestamp);
        staging.get().stage(stackTraces);
      } catch (Exception e) {
        logger.log(Level.SEVERE, e, () -> "Unexpected error during callstack sampling");
      }
    }

    private List<StackTrace> toStackTraces(
        ThreadInfo[] threadInfos, Map<Long, SamplingContext> contexts, long currentTimestamp) {
      List<StackTrace> stackTraces = new ArrayList<>(threadInfos.length);
      for (ThreadInfo threadInfo : threadInfos) {
        SamplingContext context = contexts.get(threadInfo.getThreadId());
        if (traceSamplingContexts.containsKey(context.traceId)) {
          SpanContext spanContext = retrieveActiveSpan(context.thread);
          stackTraces.add(
              toStackTrace(
                  threadInfo, context, context.traceId, spanContext.getSpanId(), currentTimestamp));
          context.updateTimestamp(currentTimestamp);
        }
      }
      return stackTraces;
    }

    private StackTrace toStackTrace(
        ThreadInfo threadInfo, SamplingContext context, long currentTimestamp) {
      SpanContext spanContext = retrieveActiveSpan(context.thread);
      return toStackTrace(
          threadInfo, context, context.traceId, spanContext.getSpanId(), currentTimestamp);
    }

    private StackTrace toStackTrace(
        ThreadInfo threadInfo,
        SamplingContext context,
        String traceId,
        String spanId,
        long currentTimestamp) {
      Duration samplingPeriod = Duration.ofNanos(currentTimestamp - context.timestamp);
      return StackTrace.from(
          Instant.now(),
          samplingPeriod,
          threadInfo,
          traceId,
          spanId,
          Thread.currentThread().getId());
    }

    private SpanContext retrieveActiveSpan(Thread thread) {
      return spanTracker.get().getActiveSpan(thread).orElse(SpanContext.getInvalid());
    }
  }

  private static class SamplingContext {
    private final Thread thread;
    private final String traceId;
    private final String spanId;
    private long timestamp;

    private SamplingContext(Thread thread, String traceId, String spanId, long timestamp) {
      this.thread = thread;
      this.traceId = traceId;
      this.spanId = spanId;
      this.timestamp = timestamp;
    }

    void updateTimestamp(long timestamp) {
      this.timestamp = timestamp;
    }
  }
}
