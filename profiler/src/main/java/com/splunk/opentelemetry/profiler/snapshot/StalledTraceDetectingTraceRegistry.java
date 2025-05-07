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
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class StalledTraceDetectingTraceRegistry implements TraceRegistry, AutoCloseable {
  private final ScheduledExecutorService scheduler =
      HelpfulExecutors.newSingleThreadedScheduledExecutor("stalled-trace-detector");
  private final Map<String, RegistrationContext> traceIds = new ConcurrentHashMap<>();
  private final TraceRegistry delegate;
  private final Supplier<StackTraceSampler> sampler;

  public StalledTraceDetectingTraceRegistry(
      TraceRegistry delegate, Supplier<StackTraceSampler> sampler, Duration stalledTimeLimit) {
    this.delegate = delegate;
    this.sampler = sampler;

    scheduler.scheduleAtFixedRate(
        removeStalledTraces(stalledTimeLimit),
        stalledTimeLimit.toMillis() / 2,
        stalledTimeLimit.toMillis() / 2,
        TimeUnit.MILLISECONDS);
  }

  @Override
  public void register(SpanContext spanContext) {
    delegate.register(spanContext);
    traceIds.put(spanContext.getTraceId(), new RegistrationContext(Instant.now(), spanContext));
  }

  @Override
  public boolean isRegistered(SpanContext spanContext) {
    return delegate.isRegistered(spanContext);
  }

  @Override
  public void unregister(SpanContext spanContext) {
    delegate.unregister(spanContext);
  }

  @Override
  public void close() throws Exception {
    scheduler.shutdown();
  }

  private Runnable removeStalledTraces(Duration stalledTimeLimit) {
    return () ->
        traceIds
            .entrySet()
            .iterator()
            .forEachRemaining(
                entry -> {
                  Instant now = Instant.now();
                  RegistrationContext context = entry.getValue();
                  Duration duration = Duration.between(now, context.registrationTime);
                  if (duration.compareTo(stalledTimeLimit) <= 0) {
                    unregister(context.spanContext);
                    sampler.get().stop(context.spanContext);
                  }
                });
  }

  private static class RegistrationContext {
    private final Instant registrationTime;
    private final SpanContext spanContext;

    private RegistrationContext(Instant registrationTime, SpanContext spanContext) {
      this.registrationTime = registrationTime;
      this.spanContext = spanContext;
    }
  }
}
