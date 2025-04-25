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

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

class AccumulatingStagingArea implements StagingArea {
  private final ConcurrentMap<String, Queue<StackTrace>> stackTraces = new ConcurrentHashMap<>();
  private final Supplier<StackTraceExporter> exporter;
  private volatile boolean closed = false;

  AccumulatingStagingArea(Supplier<StackTraceExporter> exporter) {
    this.exporter = exporter;
  }

  @Override
  public void stage(String traceId, StackTrace stackTrace) {
    if (closed) {
      return;
    }

    stackTraces.compute(
        traceId,
        (id, stackTraces) -> {
          if (stackTraces == null) {
            stackTraces = new ConcurrentLinkedQueue<>();
          }
          stackTraces.add(stackTrace);
          return stackTraces;
        });
  }

  @Override
  public void empty(String traceId) {
    Queue<StackTrace> stackTraces = this.stackTraces.remove(traceId);
    if (stackTraces != null) {
      exporter.get().export(stackTraces);
    }
  }

  @Override
  public void close() {
    closed = true;
    stackTraces.values().forEach(exporter.get()::export);
    stackTraces.clear();
  }
}
