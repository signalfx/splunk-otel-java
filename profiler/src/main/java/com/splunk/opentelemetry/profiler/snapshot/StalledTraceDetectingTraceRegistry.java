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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class StalledTraceDetectingTraceRegistry implements TraceRegistry {
  private final TraceRegistry delegate;
  private final StalledTraceDetector stalledTraceDetector;

  public StalledTraceDetectingTraceRegistry(
      TraceRegistry delegate, Supplier<StackTraceSampler> sampler, Duration stalledTimeLimit) {
    this.delegate = delegate;

    stalledTraceDetector = new StalledTraceDetector(delegate, sampler, stalledTimeLimit);
    stalledTraceDetector.setName("stalled-trace-detector");
    stalledTraceDetector.setDaemon(true);
    stalledTraceDetector.start();
  }

  @Override
  public void register(SpanContext spanContext) {
    delegate.register(spanContext);
    stalledTraceDetector.register(spanContext);
  }

  @Override
  public boolean isRegistered(SpanContext spanContext) {
    return delegate.isRegistered(spanContext);
  }

  @Override
  public void unregister(SpanContext spanContext) {
    delegate.unregister(spanContext);
    stalledTraceDetector.unregister(spanContext);
  }

  @Override
  public void close() throws Exception {
    delegate.close();
    stalledTraceDetector.shutdown();
  }

  private static class StalledTraceDetector extends Thread {
    private final Map<SpanContext, Long> traceIds = new HashMap<>();
    private final LinkedBlockingQueue<Command> queue = new LinkedBlockingQueue<>();
    private final TraceRegistry registry;
    private final Supplier<StackTraceSampler> sampler;
    private final Duration timeout;

    private boolean shutdown = false;
    private long nextStallCheck;

    StalledTraceDetector(
        TraceRegistry registry, Supplier<StackTraceSampler> sampler, Duration timeout) {
      this.registry = registry;
      this.sampler = sampler;
      this.timeout = timeout;
      updateNextExportTime();
    }

    public void register(SpanContext spanContext) {
      queue.add(Command.register(spanContext));
    }

    public void unregister(SpanContext spanContext) {
      queue.add(Command.unregister(spanContext));
    }

    public void shutdown() {
      shutdown = true;
      queue.add(Command.SHUTDOWN);
    }

    @Override
    public void run() {
      while (!shutdown) {
        try {
          Command command = queue.poll(nextStallCheck - System.nanoTime(), TimeUnit.NANOSECONDS);
          if (command != null) {
            if (command.action == Action.SHUTDOWN) {
              traceIds.clear();
              return;
            } else if(command.action == Action.REGISTER) {
              traceIds.put(command.spanContext, System.nanoTime() + timeout.toNanos());
            } else if (command.action == Action.UNREGISTER) {
              traceIds.remove(command.spanContext);
            }
          }
          removeStalledTraces();
          updateNextExportTime();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }

    private void removeStalledTraces() {
      traceIds.entrySet().stream()
          .filter(entry -> entry.getValue() <= System.nanoTime())
          .map(Map.Entry::getKey)
          .iterator()
          .forEachRemaining(
              spanContext -> {
                registry.unregister(spanContext);
                sampler.get().stop(spanContext);
              });
    }

    private void updateNextExportTime() {
      nextStallCheck = System.nanoTime() + timeout.toNanos();
    }

    private static class Command {
      public static final Command SHUTDOWN = new Command(Action.SHUTDOWN, null);

      public static Command register(SpanContext spanContext) {
        return new Command(Action.REGISTER, spanContext);
      }

      public static Command unregister(SpanContext spanContext) {
        return new Command(Action.UNREGISTER, spanContext);
      }

      private final Action action;
      private final SpanContext spanContext;

      private Command(Action action, SpanContext spanContext) {
        this.action = action;
        this.spanContext = spanContext;
      }
    }

    private enum Action {
      REGISTER,
      UNREGISTER,
      SHUTDOWN
    }
  }
}
