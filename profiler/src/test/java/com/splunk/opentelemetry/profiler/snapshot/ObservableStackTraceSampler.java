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

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Test-only implementation of the {@link StackTraceSampler}. Records which thread IDs have been
 * marked for sampling and cleans up after each test.
 */
public class ObservableStackTraceSampler implements StackTraceSampler {
  private final Set<Long> threadIds = new HashSet<>();

  @Override
  public void startSampling(String traceId, long threadId) {
    threadIds.add(threadId);
  }

  @Override
  public void stopSampling(long threadId) {
    threadIds.remove(threadId);
  }

  boolean isBeingSampled(long threadId) {
    return threadIds.contains(threadId);
  }

  //  @Override
  public void afterEach(ExtensionContext extensionContext) {
    threadIds.clear();
  }
}
