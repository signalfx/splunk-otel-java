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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

class PeriodicStackTraceSampler implements StackTraceSampler {
  private static final Logger logger = Logger.getLogger(PeriodicStackTraceSampler.class.getName());

  private final ThreadSampler sampler;
  private volatile boolean closed;

  public PeriodicStackTraceSampler(
      Supplier<StagingArea> staging, Supplier<SpanTracker> spanTracker, Duration samplingPeriod) {
    this(staging, spanTracker, new ThreadInfoCollector(), samplingPeriod);
  }

  @VisibleForTesting
  PeriodicStackTraceSampler(
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
    if (closed) {
      return;
    }
    sampler.add(Thread.currentThread(), spanContext);
  }

  @Override
  public void stop(SpanContext spanContext) {
    if (closed) {
      return;
    }
    sampler.remove(Thread.currentThread(), spanContext);
  }

  @Override
  public void close() {
    this.closed = true;
    // Wait for the sampling thread to exit. Note that this does not guarantee an
    // immediate shutdown as the sampling thread may be actively staging stack traces
    // when the shutdown request is made. If this is the case, the thread will shutdown
    // upon completion of the sample.
    try {
      sampler.shutdown();
      sampler.join();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }
  }

  private static class ThreadSampler extends Thread {
    private final Map<Thread, SamplingContext> threadSamplingContexts = new ConcurrentHashMap<>();
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
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

    void add(Thread thread, SpanContext spanContext) {
      threadSamplingContexts.computeIfAbsent(thread, t -> {
        SamplingContext context = new SamplingContext(t, spanContext, System.nanoTime());
        takeOnDemandSample(context).ifPresent(staging.get()::stage);
        return context;
      });
    }

    void remove(Thread thread, SpanContext spanContext) {
      threadSamplingContexts.computeIfPresent(thread, (t, context) -> {
        if (context.spanContext.equals(spanContext)) {
          takeOnDemandSample(context).ifPresent(staging.get()::stage);
          return null;
        }
        return context;
      });
    }

    private Optional<StackTrace> takeOnDemandSample(SamplingContext context) {
      long currentSampleTime = System.nanoTime();
      // When the context is locked, the periodic sampling thread is actively reporting
      // a sample. No need to report both so skip the on-demand sample.
      if (context.lock.tryLock()) {
        try {
          ThreadInfo threadInfo = collector.getThreadInfo(context.thread.getId());
          SpanContext spanContext = retrieveActiveSpan(context.thread);
          return toStackTrace(threadInfo, context, spanContext.getSpanId(), currentSampleTime);
        } finally {
          context.lock.unlock();
        }
      }
      return Optional.empty();
    }

    void shutdown() {
      threadSamplingContexts.clear();
      shutdownLatch.countDown();
    }

    @Override
    public void run() {
      try {
        while (!Thread.currentThread().isInterrupted()) {
          boolean shutdown = shutdownLatch.await(delay.toNanos(), TimeUnit.NANOSECONDS);
          if (shutdown) {
            return;
          }
          sample(threadSamplingContexts.values());
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
      long currentSampleTime = System.nanoTime();
      try {
        ThreadInfo[] threadInfos = collector.getThreadInfo(threadContexts.keySet());
        List<StackTrace> stackTraces =
            toStackTraces(threadInfos, threadContexts, currentSampleTime);
        staging.get().stage(stackTraces);
      } catch (Exception e) {
        logger.info("Unexpected error during callstack sampling");
      }
    }

    private List<StackTrace> toStackTraces(
        ThreadInfo[] threadInfos, Map<Long, SamplingContext> contexts, long currentSampleTime) {
      List<StackTrace> stackTraces = new ArrayList<>(threadInfos.length);
      for (ThreadInfo threadInfo : threadInfos) {
        SamplingContext context = contexts.get(threadInfo.getThreadId());
        // When the context is locked an on demand sample is being taken. No need to report
        // both so skip the periodic sample.
        if (context.lock.tryLock()) {
          try {
            SpanContext spanContext = retrieveActiveSpan(context.thread);
            toStackTrace(threadInfo, context, spanContext.getSpanId(), currentSampleTime)
                .ifPresent(stackTraces::add);
          } finally {
            context.lock.unlock();
          }
        }
      }
      return stackTraces;
    }

    private Optional<StackTrace> toStackTrace(
        ThreadInfo threadInfo, SamplingContext context, String spanId, long currentSampleTime) {
      // If multiple threads have managed to take a sample for the same context
      // one of the sampling periods may be a negative value. If this happens a
      // previous sample fully encompasses this sample and so this sample can
      // be safely dropped.
      Duration samplingPeriod = Duration.ofNanos(currentSampleTime - context.sampleTime);
      if (samplingPeriod.isNegative()) {
        return Optional.empty();
      }
      context.updateSampleTime(currentSampleTime);
      return Optional.of(
          StackTrace.from(
              Instant.now(),
              samplingPeriod,
              threadInfo,
              context.spanContext.getTraceId(),
              spanId,
              Thread.currentThread().getId()));
    }

    /** It's possible the active span will have changed since the sample was taken */
    private SpanContext retrieveActiveSpan(Thread thread) {
      return spanTracker.get().getActiveSpan(thread).orElse(SpanContext.getInvalid());
    }
  }

  private static class SamplingContext {
    private final Lock lock = new ReentrantLock();
    private final Thread thread;
    private final SpanContext spanContext;
    private long sampleTime;

    private SamplingContext(Thread thread, SpanContext spanContext, long sampleTime) {
      this.thread = thread;
      this.spanContext = spanContext;
      this.sampleTime = sampleTime;
    }

    void updateSampleTime(long sampleTime) {
      this.sampleTime = sampleTime;
    }
  }
}
