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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.splunk.opentelemetry.profiler.exporter.InMemoryOtelLogger;
import io.opentelemetry.api.logs.Logger;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;

class AsyncStackTraceExporterTest {
  private final InMemoryOtelLogger logger = new InMemoryOtelLogger();
  private final ObservableRunnable worker = new ObservableRunnable();
  private final AsyncStackTraceExporter exporter = new AsyncStackTraceExporter(logger, worker);

  @Test
  void exportStackTrace() {
    var stackTrace = Snapshotting.stackTrace().build();

    exporter.export(List.of(stackTrace));
    await().atMost(Duration.ofSeconds(5)).until(() -> !worker.stackTraces().isEmpty());

    assertEquals(List.of(stackTrace), worker.stackTraces());
    assertEquals(1, worker.invocations());
  }

  @Test
  void exportStackTraceMultipleTimes() {
    var stackTrace = Snapshotting.stackTrace().build();

    exporter.export(List.of(stackTrace));
    exporter.export(List.of(stackTrace));
    exporter.export(List.of(stackTrace));
    await().atMost(Duration.ofSeconds(5)).until(() -> !worker.stackTraces().isEmpty());

    assertEquals(3, worker.invocations());
  }

  static final class ObservableRunnable
      implements Runnable, BiFunction<Logger, List<StackTrace>, Runnable> {
    private final List<StackTrace> stackTraces = new CopyOnWriteArrayList<>();
    private final AtomicInteger invocations = new AtomicInteger();

    @Override
    public void run() {
      invocations.incrementAndGet();
    }

    @Override
    public Runnable apply(Logger logger, List<StackTrace> stackTraces) {
      this.stackTraces.addAll(stackTraces);
      return this;
    }

    List<StackTrace> stackTraces() {
      return Collections.unmodifiableList(stackTraces);
    }

    int invocations() {
      return invocations.get();
    }
  }
}
