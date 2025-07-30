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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.test.utils.GcUtils;
import java.lang.ref.WeakReference;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class OrphanedTraceCleanerTest {
  private final TraceRegistry registry = new TraceRegistry();
  private final OrphanedTraceCleaner cleaner = new OrphanedTraceCleaner(registry);

  @AfterEach
  void teardown() {
    cleaner.close();
  }

  @Test
  void registerAndUnregister() {
    var spanContext = Snapshotting.spanContext().build();
    var span = Span.wrap(spanContext);
    cleaner.register(span);

    assertThat(cleaner.getTracesSize()).isEqualTo(1);

    cleaner.unregister(spanContext);

    assertThat(cleaner.getTracesSize()).isEqualTo(0);
  }

  @Test
  void unregisterOrphanedTraces() throws Exception {
    var spanContext = Snapshotting.spanContext().build();
    registry.register(spanContext);
    var span = Span.wrap(spanContext);
    cleaner.register(span);

    var spanReference = new WeakReference<>(span);
    span = null;
    GcUtils.awaitGc(spanReference, Duration.ofSeconds(10));

    await().untilAsserted(() -> assertThat(registry.isRegistered(spanContext)).isFalse());
  }
}
