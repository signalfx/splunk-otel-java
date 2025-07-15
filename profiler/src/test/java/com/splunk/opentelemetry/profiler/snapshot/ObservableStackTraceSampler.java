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
import java.util.HashSet;
import java.util.Set;

/**
 * Test-only implementation of the {@link StackTraceSampler}. Records which thread IDs have been
 * marked for sampling and cleans up after each test.
 */
public class ObservableStackTraceSampler implements StackTraceSampler {
  private final Set<String> traceIds = new HashSet<>();
  private final Set<Thread> threads = new HashSet<>();

  @Override
  public void start(Thread thread, SpanContext spanContext) {
    traceIds.add(spanContext.getTraceId());
    threads.add(thread);
  }

  @Override
  public void stop(Thread thread, SpanContext spanContext) {
    traceIds.remove(spanContext.getTraceId());
    threads.remove(thread);
  }

  @Override
  public boolean isBeingSampled(Thread thread) {
    return threads.contains(thread);
  }

  boolean isBeingSampled(SpanContext spanContext) {
    return traceIds.contains(spanContext.getTraceId());
  }
}
