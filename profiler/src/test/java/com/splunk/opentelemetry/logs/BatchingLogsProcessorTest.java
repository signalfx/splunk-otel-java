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
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BatchingLogsProcessorTest {

  LogEntry log1, log2, log3;

  @BeforeEach
  void setup() {
    log1 =
        LogEntry.builder()
            .attributes(Attributes.of(AttributeKey.stringKey("one"), "one"))
            .body("foo")
            .build();
    log2 =
        LogEntry.builder()
            .attributes(Attributes.of(AttributeKey.stringKey("two"), "two"))
            .body("bar")
            .build();
    log3 =
        LogEntry.builder()
            .attributes(Attributes.of(AttributeKey.stringKey("three"), "three"))
            .body("baz")
            .build();
  }

  @Test
  void testSimpleActionWhenSizeReached() {
    List<LogEntry> seen = new ArrayList<>();
    Consumer<List<LogEntry>> action = seen::addAll;
    BatchingLogsProcessor processor = new BatchingLogsProcessor(Duration.ofHours(15), 3, action);
    processor.start();
    processor.log(log1);
    assertTrue(seen.isEmpty());
    processor.log(log2);
    assertTrue(seen.isEmpty());
    processor.log(log3);
    assertEquals(Arrays.asList(log1, log2, log3), seen);
  }

  @Test
  void testSimpleActionWhenTimeReached() throws Exception {
    List<LogEntry> seen = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(1);
    Consumer<List<LogEntry>> action =
        logs -> {
          seen.addAll(logs);
          latch.countDown();
        };
    BatchingLogsProcessor processor =
        new BatchingLogsProcessor(Duration.ofMillis(50), 3000, action);
    processor.start();
    processor.log(log1);
    processor.log(log2);
    processor.log(log3);
    assertTrue(latch.await(5, SECONDS));
    assertEquals(Arrays.asList(log1, log2, log3), seen);
  }

  @Test
  void testActionNotCalledWhenEmptyAfterTime() throws Exception {
    List<LogEntry> seen = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(1);
    Consumer<List<LogEntry>> action =
        logs -> {
          seen.addAll(logs);
          latch.countDown();
        };
    BatchingLogsProcessor processor = new BatchingLogsProcessor(Duration.ofMillis(1), 3000, action);
    processor.start();
    assertFalse(latch.await(1, SECONDS));
    assertTrue(seen.isEmpty());
  }

  @Test
  void testStopDoesAction() throws Exception {
    List<LogEntry> seen = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(1);
    Consumer<List<LogEntry>> action =
        logs -> {
          seen.addAll(logs);
          latch.countDown();
        };
    BatchingLogsProcessor processor =
        new BatchingLogsProcessor(Duration.ofSeconds(60), 3000, action);
    processor.start();
    processor.log(log1);
    processor.log(log2);
    processor.stop();
    assertTrue(latch.await(5, SECONDS));
    assertEquals(Arrays.asList(log1, log2), seen);
  }

  @Test
  void testStopBeforeStart() {
    Consumer<List<LogEntry>> action = logs -> {};
    BatchingLogsProcessor processor =
        new BatchingLogsProcessor(Duration.ofSeconds(60), 3000, action);
    assertThrows(IllegalStateException.class, processor::stop);
  }

  @Test
  void testMultipleStarts() {
    Consumer<List<LogEntry>> action = logs -> {};
    BatchingLogsProcessor processor =
        new BatchingLogsProcessor(Duration.ofSeconds(60), 3000, action);
    processor.start();
    assertThrows(IllegalStateException.class, processor::start);
  }
}
