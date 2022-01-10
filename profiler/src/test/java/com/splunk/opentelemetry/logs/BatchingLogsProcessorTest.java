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

package com.splunk.opentelemetry.logs;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.data.LogDataBuilder;
import io.opentelemetry.sdk.logs.export.LogExporter;
import io.opentelemetry.sdk.resources.Resource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BatchingLogsProcessorTest {

  MockExporter exporter;

  LogData log1, log2, log3;

  @BeforeEach
  void setup() {
    LogDataBuilder builder =
        LogDataBuilder.create(
            Resource.getDefault(), InstrumentationLibraryInfo.create("test", "1.2.3"));
    log1 =
        builder
            .setAttributes(Attributes.of(AttributeKey.stringKey("one"), "one"))
            .setBody("foo")
            .build();
    log2 =
        builder
            .setAttributes(Attributes.of(AttributeKey.stringKey("two"), "two"))
            .setBody("bar")
            .build();
    log3 =
        builder
            .setAttributes(Attributes.of(AttributeKey.stringKey("three"), "three"))
            .setBody("baz")
            .build();
    exporter = new MockExporter();
  }

  @Test
  void testSimpleActionWhenSizeReached() throws Exception {
    BatchingLogsProcessor processor =
        BatchingLogsProcessor.builder()
            .maxTimeBetweenBatches(Duration.ofHours(15))
            .maxBatchSize(3)
            .batchAction(exporter)
            .build();
    processor.start();
    processor.emit(log1);
    assertTrue(exporter.seen.isEmpty());
    processor.emit(log2);
    assertTrue(exporter.seen.isEmpty());
    processor.emit(log3);
    assertTrue(exporter.latch.await(5, SECONDS));
    assertEquals(Arrays.asList(log1, log2, log3), exporter.seen);
  }

  @Test
  void testSimpleActionWhenTimeReached() throws Exception {

    BatchingLogsProcessor processor =
        BatchingLogsProcessor.builder()
            .maxTimeBetweenBatches(Duration.ofMillis(50))
            .maxBatchSize(3000)
            .batchAction(exporter)
            .build();

    processor.start();
    processor.emit(log1);
    processor.emit(log2);
    processor.emit(log3);
    assertTrue(exporter.latch.await(5, SECONDS));
    assertEquals(Arrays.asList(log1, log2, log3), exporter.seen);
  }

  @Test
  void testActionNotCalledWhenEmptyAfterTime() throws Exception {
    BatchingLogsProcessor processor =
        BatchingLogsProcessor.builder()
            .maxTimeBetweenBatches(Duration.ofMillis(1))
            .maxBatchSize(3000)
            .batchAction(exporter)
            .build();

    processor.start();
    assertFalse(exporter.latch.await(1, SECONDS));
    assertTrue(exporter.seen.isEmpty());
  }

  @Test
  void testStopDoesAction() throws Exception {
    BatchingLogsProcessor processor =
        BatchingLogsProcessor.builder()
            .maxTimeBetweenBatches(Duration.ofSeconds(60))
            .maxBatchSize(3000)
            .batchAction(exporter)
            .build();

    processor.start();
    processor.emit(log1);
    processor.emit(log2);
    processor.stop();
    assertTrue(exporter.latch.await(5, SECONDS));
    assertEquals(Arrays.asList(log1, log2), exporter.seen);
  }

  @Test
  void testStopBeforeStart() {
    BatchingLogsProcessor processor =
        BatchingLogsProcessor.builder()
            .maxTimeBetweenBatches(Duration.ofSeconds(60))
            .maxBatchSize(3000)
            .batchAction(exporter)
            .build();

    assertThrows(IllegalStateException.class, processor::stop);
  }

  @Test
  void testMultipleStarts() {
    BatchingLogsProcessor processor =
        BatchingLogsProcessor.builder()
            .maxTimeBetweenBatches(Duration.ofSeconds(60))
            .maxBatchSize(3000)
            .batchAction(exporter)
            .build();

    processor.start();
    assertThrows(IllegalStateException.class, processor::start);
  }

  static class MockExporter implements LogExporter {

    List<LogData> seen = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(1);

    @Override
    public CompletableResultCode export(Collection<LogData> logs) {
      seen.addAll(logs);
      latch.countDown();
      return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
      return null;
    }

    @Override
    public CompletableResultCode shutdown() {
      return null;
    }
  }
}
