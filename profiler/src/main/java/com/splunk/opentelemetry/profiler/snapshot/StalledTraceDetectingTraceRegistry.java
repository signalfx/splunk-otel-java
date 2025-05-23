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
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class StalledTraceDetectingTraceRegistry implements TraceRegistry {
  private final ScheduledExecutorService scheduler =
      HelpfulExecutors.newSingleThreadedScheduledExecutor("stalled-trace-detector");
  private final Map<SpanContext, Long> traceIds = new ConcurrentHashMap<>();
  private final TraceRegistry delegate;
  private final Supplier<StackTraceSampler> sampler;
  private final Duration stalledTimeLimit;

  public StalledTraceDetectingTraceRegistry(
      TraceRegistry delegate, Supplier<StackTraceSampler> sampler, Duration stalledTimeLimit) {
    this.delegate = delegate;
    this.sampler = sampler;
    this.stalledTimeLimit = stalledTimeLimit;

    scheduler.scheduleAtFixedRate(
        removeStalledTraces(),
        stalledTimeLimit.toMillis() / 2,
        stalledTimeLimit.toMillis() / 2,
        TimeUnit.MILLISECONDS);
  }

  @Override
  public void register(SpanContext spanContext) {
    delegate.register(spanContext);
    traceIds.put(spanContext, System.nanoTime() + stalledTimeLimit.toNanos());
  }

  @Override
  public boolean isRegistered(SpanContext spanContext) {
    return delegate.isRegistered(spanContext);
  }

  @Override
  public void unregister(SpanContext spanContext) {
    delegate.unregister(spanContext);
    traceIds.remove(spanContext);
  }

  @Override
  public void close() throws Exception {
    scheduler.shutdownNow();
    delegate.close();
  }

  private Runnable removeStalledTraces() {
    return () -> traceIds.entrySet().stream()
        .filter(entry -> entry.getValue() <= System.nanoTime())
        .map(Map.Entry::getKey)
        .iterator()
        .forEachRemaining(spanContext -> {
          unregister(spanContext);
          sampler.get().stop(spanContext);
        });
  }
}
