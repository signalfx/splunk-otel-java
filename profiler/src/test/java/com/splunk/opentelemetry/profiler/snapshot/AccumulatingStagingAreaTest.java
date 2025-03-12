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

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.sdk.trace.IdGenerator;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccumulatingStagingAreaTest {
  private final IdGenerator idGenerator = IdGenerator.random();
  private final InMemoryStackTraceExporter exporter = new InMemoryStackTraceExporter();
  private final AccumulatingStagingArea stagingArea = new AccumulatingStagingArea(() -> exporter);

  @Test
  void exportStackTracesToLogExporter() {
    var traceId = idGenerator.generateTraceId();
    var stackTrace = Snapshotting.stackTrace().build();

    stagingArea.stage(traceId, stackTrace);
    stagingArea.empty(traceId);

    assertEquals(List.of(stackTrace), exporter.stackTraces());
  }

  @Test
  void onlyExportStackTracesWhenAtLeastOneHasBeenStaged() {
    var traceId = idGenerator.generateTraceId();
    stagingArea.empty(traceId);
    assertEquals(Collections.emptyList(), exporter.stackTraces());
  }

  @Test
  void exportMultipleStackTracesToLogExporter() {
    var traceId = idGenerator.generateTraceId();
    var stackTrace1 = Snapshotting.stackTrace().withId(1).withName("one").build();
    var stackTrace2 = Snapshotting.stackTrace().withId(1).withName("two").build();

    stagingArea.stage(traceId, stackTrace1);
    stagingArea.stage(traceId, stackTrace2);
    stagingArea.empty(traceId);

    assertEquals(List.of(stackTrace1, stackTrace2), exporter.stackTraces());
  }

  @Test
  void exportStackTracesForOnlySpecifiedThread() {
    var traceId1 = idGenerator.generateTraceId();
    var traceId2 = idGenerator.generateTraceId();
    var stackTrace1 = Snapshotting.stackTrace().withId(1).withName("one").build();
    var stackTrace2 = Snapshotting.stackTrace().withId(1).withName("two").build();

    stagingArea.stage(traceId1, stackTrace1);
    stagingArea.stage(traceId2, stackTrace2);
    stagingArea.empty(traceId1);

    assertEquals(List.of(stackTrace1), exporter.stackTraces());
  }

  @Test
  void exportStackTracesForMultipleThreads() {
    var traceId1 = idGenerator.generateTraceId();
    var traceId2 = idGenerator.generateTraceId();
    var stackTrace1 = Snapshotting.stackTrace().withId(1).withName("one").build();
    var stackTrace2 = Snapshotting.stackTrace().withId(1).withName("two").build();

    stagingArea.stage(traceId1, stackTrace1);
    stagingArea.stage(traceId2, stackTrace2);
    stagingArea.empty(traceId1);
    stagingArea.empty(traceId2);

    assertEquals(List.of(stackTrace1, stackTrace2), exporter.stackTraces());
  }

  @Test
  void stackTracesAreNotExportedMultipleTimes() {
    var traceId = idGenerator.generateTraceId();
    var stackTrace = Snapshotting.stackTrace().build();

    stagingArea.stage(traceId, stackTrace);
    stagingArea.empty(traceId);
    stagingArea.empty(traceId);

    assertEquals(List.of(stackTrace), exporter.stackTraces());
  }
}
