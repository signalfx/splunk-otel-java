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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * In memory implementation of the {@link StagingArea} interface that allows for direct access to
 * the stockpiled {@link StackTrace}s. Intended for testing use only.
 */
class InMemoryStagingArea implements StagingArea, AfterEachCallback {
  private final Map<Long, List<StackTrace>> stackTraces = new ConcurrentHashMap<>();

  @Override
  public void stage(long threadId, StackTrace stackTrace) {
    stackTraces.compute(
        threadId,
        (id, stackTraces) -> {
          if (stackTraces == null) {
            stackTraces = new ArrayList<>();
          }
          stackTraces.add(stackTrace);
          return stackTraces;
        });
  }

  @Override
  public void empty(long threadId) {
    stackTraces.remove(threadId);
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) {
    stackTraces.clear();
  }

  public List<StackTrace> allStackTraces() {
    return stackTraces.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
  }
}
