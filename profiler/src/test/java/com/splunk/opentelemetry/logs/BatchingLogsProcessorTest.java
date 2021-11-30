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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import io.opentelemetry.sdk.logs.data.LogData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BatchingLogsProcessorTest {

  LogEntry log1, log2, log3;

  @BeforeEach
  void setup() {
    log1 =
        LogEntry.builder()
            .attributes(Attributes.of(AttributeKey.stringKey("one"), "one"))
            .bodyString("foo")
            .build();
    log2 =
        LogEntry.builder()
            .attributes(Attributes.of(AttributeKey.stringKey("two"), "two"))
            .bodyString("bar")
            .build();
    log3 =
        LogEntry.builder()
            .attributes(Attributes.of(AttributeKey.stringKey("three"), "three"))
            .bodyString("baz")
            .build();
  }

  @Test
  void testSimpleActionWhenSizeReached() throws Exception {
    List<LogData> seen = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(1);
    LogsExporter exporter =
        logs -> {
          seen.addAll(logs);
          latch.countDown();
        };
    BatchingLogsProcessor processor =
        BatchingLogsProcessor.builder()
            .maxTimeBetweenBatches(Duration.ofHours(15))
            .maxBatchSize(3)
            .batchAction(exporter)
            .build();
    processor.start();
    processor.log(log1);
    assertTrue(seen.isEmpty());
    processor.log(log2);
    assertTrue(seen.isEmpty());
    processor.log(log3);
    assertTrue(latch.await(5, SECONDS));
    assertEquals(Arrays.asList(log1, log2, log3), seen);
  }

  @Test
  void testSimpleActionWhenTimeReached() throws Exception {
    List<LogData> seen = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(1);
    LogsExporter exporter =
        logs -> {
          seen.addAll(logs);
          latch.countDown();
        };

    BatchingLogsProcessor processor =
        BatchingLogsProcessor.builder()
            .maxTimeBetweenBatches(Duration.ofMillis(50))
            .maxBatchSize(3000)
            .batchAction(exporter)
            .build();

    processor.start();
    processor.log(log1);
    processor.log(log2);
    processor.log(log3);
    assertTrue(latch.await(5, SECONDS));
    assertEquals(Arrays.asList(log1, log2, log3), seen);
  }

  @Test
  void testActionNotCalledWhenEmptyAfterTime() throws Exception {
    List<LogData> seen = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(1);
    LogsExporter exporter =
        logs -> {
          seen.addAll(logs);
          latch.countDown();
        };
    BatchingLogsProcessor processor =
        BatchingLogsProcessor.builder()
            .maxTimeBetweenBatches(Duration.ofMillis(1))
            .maxBatchSize(3000)
            .batchAction(exporter)
            .build();

    processor.start();
    assertFalse(latch.await(1, SECONDS));
    assertTrue(seen.isEmpty());
  }

  @Test
  void testStopDoesAction() throws Exception {
    List<LogData> seen = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(1);
    LogsExporter exporter =
        logs -> {
          seen.addAll(logs);
          latch.countDown();
        };
    BatchingLogsProcessor processor =
        BatchingLogsProcessor.builder()
            .maxTimeBetweenBatches(Duration.ofSeconds(60))
            .maxBatchSize(3000)
            .batchAction(exporter)
            .build();

    processor.start();
    processor.log(log1);
    processor.log(log2);
    processor.stop();
    assertTrue(latch.await(5, SECONDS));
    assertEquals(Arrays.asList(log1, log2), seen);
  }

  @Test
  void testStopBeforeStart() {
    LogsExporter exporter = logs -> {};
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
    LogsExporter exporter = logs -> {};
    BatchingLogsProcessor processor =
        BatchingLogsProcessor.builder()
            .maxTimeBetweenBatches(Duration.ofSeconds(60))
            .maxBatchSize(3000)
            .batchAction(exporter)
            .build();

    processor.start();
    assertThrows(IllegalStateException.class, processor::start);
  }
}
