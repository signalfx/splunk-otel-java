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
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * In memory implementation of the {@link StagingArea} interface that allows for direct access to
 * the stockpiled {@link StackTrace}s. Intended for testing use only.
 */
class InMemoryStagingArea implements StagingArea {
  private final ConcurrentMap<String, StagedStackTraces> stackTraces = new ConcurrentHashMap<>();

  @Override
  public void stage(StackTrace stackTrace) {
    stackTraces.compute(
        stackTrace.getTraceId(),
        (id, stackTraces) -> {
          if (stackTraces == null) {
            stackTraces = new StagedStackTraces();
          }
          if (!stackTraces.emptied()) {
            stackTraces.add(stackTrace);
          }
          return stackTraces;
        });
  }

  public void empty() {
    stackTraces.clear();
  }

  StagedStackTraces getStackTraces(SpanContext spanContext) {
    return stackTraces.getOrDefault(spanContext.getTraceId(), new StagedStackTraces());
  }

  boolean hasStackTraces(SpanContext spanContext) {
    return !getStackTraces(spanContext).isEmpty();
  }

  public List<StackTrace> allStackTraces() {
    return stackTraces.values().stream()
        .map(StagedStackTraces::stackTraces)
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  static class StagedStackTraces extends AbstractList<StackTrace> {
    private final List<StackTrace> stackTraces = new ArrayList<>();
    private boolean emptied = false;

    List<StackTrace> stackTraces() {
      return stackTraces;
    }

    @Override
    public int size() {
      return stackTraces.size();
    }

    @Override
    public boolean add(StackTrace stackTrace) {
      return stackTraces.add(stackTrace);
    }

    @Override
    public StackTrace get(int index) {
      return stackTraces.get(index);
    }

    boolean emptied() {
      return emptied;
    }

    private void empty() {
      emptied = true;
    }
  }
}
