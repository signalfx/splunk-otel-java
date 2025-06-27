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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
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
    sampler = new ThreadSampler(staging, spanTracker, samplingPeriod);
    sampler.setName("daemon-thread-stack-trace-sampler");
    sampler.setDaemon(true);
    sampler.start();
  }

  @Override
  public void start(SpanContext spanContext) {
    sampler.add(Thread.currentThread(), spanContext);
  }

  @Override
  public void stop(SpanContext spanContext) {
    sampler.remove(Thread.currentThread(), spanContext);
  }

  @Override
  public void close() {
    sampler.shutdown();
  }

  private static class ThreadSampler extends Thread {
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final BlockingQueue<Command> queue = new LinkedBlockingQueue<>();
    private final Supplier<StagingArea> staging;
    private final Supplier<SpanTracker> spanTracker;
    private final Duration delay;

    private ThreadSampler(
        Supplier<StagingArea> staging, Supplier<SpanTracker> spanTracker, Duration delay) {
      this.staging = staging;
      this.spanTracker = spanTracker;
      this.delay = delay;
    }

    void add(Thread thread, SpanContext spanContext) {
      queue.add(new Command(Action.START, spanContext, thread));
    }

    void remove(Thread thread, SpanContext spanContext) {
      queue.add(new Command(Action.STOP, spanContext, thread));
    }

    void shutdown() {
      queue.add(new Command(Action.SHUTDOWN, null, null));
    }

    @Override
    public void run() {
      Map<String, SamplingContext> traceThreads = new HashMap<>();
      long nextSampleTime = System.nanoTime() + delay.toNanos();
      try {
        while (!Thread.currentThread().isInterrupted()) {
          Command command = queue.poll(nextSampleTime - System.nanoTime(), TimeUnit.NANOSECONDS);
          if (command == null) {
            sample(traceThreads.values());
          } else {
            if (command.action == Action.SHUTDOWN) {
              return;
            } else if (command.action == Action.START) {
              startSampling(command, traceThreads);
            } else if (command.action == Action.STOP) {
              stopSampling(command, traceThreads);
            }
          }
          nextSampleTime = System.nanoTime() + delay.toNanos();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    private void startSampling(Command command, Map<String, SamplingContext> traceThreads) {
      traceThreads.computeIfAbsent(
          command.spanContext.getTraceId(),
          traceId -> {
            SamplingContext context =
                new SamplingContext(command.thread, command.spanContext, System.nanoTime());
            sample(Collections.singletonList(context));
            return context;
          });
    }

    private void stopSampling(Command command, Map<String, SamplingContext> traceThreads) {
      traceThreads.computeIfPresent(
          command.spanContext.getTraceId(),
          (traceId, context) -> {
            if (command.spanContext.equals(context.spanContext)) {
              sample(Collections.singletonList(context));
              return null;
            }
            return context;
          });
    }

    private void sample(Collection<SamplingContext> contexts) {
      Map<Long, SamplingContext> threadSamplingContexts =
          contexts.stream().collect(Collectors.toMap(c -> c.thread.getId(), context -> context));
      long[] threadIds =
          threadSamplingContexts.keySet().stream().mapToLong(Long::longValue).toArray();

      long currentSampleTimestamp = System.nanoTime();
      try {
        ThreadInfo[] threadInfos = captureStackTraces(threadIds);
        List<StackTrace> stackTraces =
            toStackTraces(threadInfos, threadSamplingContexts, currentSampleTimestamp);
        stage(stackTraces);
      } catch (Exception e) {
        logger.log(Level.SEVERE, e, () -> "Unexpected error during callstack sampling");
      }
    }

    private ThreadInfo[] captureStackTraces(long[] threadIds) {
      try {
        return threadMXBean.getThreadInfo(threadIds, Integer.MAX_VALUE);
      } catch (Exception e) {
        logger.log(
            Level.SEVERE,
            e,
            () ->
                "Error taking callstack samples for thread ids ["
                    + Arrays.toString(threadIds)
                    + "]");
      }
      return new ThreadInfo[0];
    }

    private List<StackTrace> toStackTraces(
        ThreadInfo[] threadInfos, Map<Long, SamplingContext> contexts, long currentTimestamp) {
      List<StackTrace> stackTraces = new ArrayList<>(threadInfos.length);
      for (ThreadInfo threadInfo : threadInfos) {
        SamplingContext context = contexts.get(threadInfo.getThreadId());
        stackTraces.add(toStackTrace(threadInfo, context, currentTimestamp));
      }
      return stackTraces;
    }

    private StackTrace toStackTrace(
        ThreadInfo threadInfo, SamplingContext context, long currentTimestamp) {
      Duration samplingPeriod = Duration.ofNanos(currentTimestamp - context.timestamp);
      context.updateTimestamp(currentTimestamp);
      String spanId = retrieveActiveSpan(context.thread);
      return StackTrace.from(
          Instant.now(), samplingPeriod, threadInfo, context.spanContext.getTraceId(), spanId);
    }

    private void stage(Collection<StackTrace> stackTraces) {
      try {
        staging.get().stage(stackTraces);
      } catch (Exception e) {
        logger.log(Level.SEVERE, e, stagingErrorMessage(stackTraces));
      }
    }

    private String retrieveActiveSpan(Thread thread) {
      return spanTracker.get().getActiveSpan(thread).orElse(SpanContext.getInvalid()).getSpanId();
    }

    private Supplier<String> stagingErrorMessage(Collection<StackTrace> stackTraces) {
      return () ->
          "Exception thrown attempting to stage callstacks for trace ids " + traceIds(stackTraces);
    }

    private String traceIds(Collection<StackTrace> stackTraces) {
      return stackTraces.stream().map(StackTrace::getTraceId).collect(Collectors.joining(","));
    }
  }

  private static class Command {
    private final Action action;
    private final SpanContext spanContext;
    private final Thread thread;

    private Command(Action action, SpanContext spanContext, Thread thread) {
      this.action = action;
      this.spanContext = spanContext;
      this.thread = thread;
    }
  }

  private enum Action {
    START,
    STOP,
    SHUTDOWN
  }

  private static class SamplingContext {
    private final Thread thread;
    private final SpanContext spanContext;
    private long timestamp;

    private SamplingContext(Thread thread, SpanContext spanContext, long timestamp) {
      this.thread = thread;
      this.spanContext = spanContext;
      this.timestamp = timestamp;
    }

    void updateTimestamp(long timestamp) {
      this.timestamp = timestamp;
    }
  }
}
