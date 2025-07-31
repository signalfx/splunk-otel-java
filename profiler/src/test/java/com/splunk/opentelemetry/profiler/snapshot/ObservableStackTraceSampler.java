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
import java.util.HashMap;
import java.util.Map;

/**
 * Test-only implementation of the {@link StackTraceSampler}. Records which thread IDs have been
 * marked for sampling and cleans up after each test.
 */
public class ObservableStackTraceSampler implements StackTraceSampler {
  private final Map<Thread, String> threads = new HashMap<>();

  @Override
  public void start(Thread thread, String traceId) {
    threads.put(thread, traceId);
  }

  @Override
  public void stop(Thread thread) {
    threads.remove(thread);
  }

  @Override
  public boolean isBeingSampled(Thread thread) {
    return threads.containsKey(thread);
  }

  boolean isBeingSampled(SpanContext spanContext) {
    return threads.containsValue(spanContext.getTraceId());
  }
}
