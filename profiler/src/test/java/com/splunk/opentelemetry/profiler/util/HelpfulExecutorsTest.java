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

package com.splunk.opentelemetry.profiler.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class HelpfulExecutorsTest {

  Function<String, Logger> originalLoggerSupplier;

  @BeforeEach
  void setup() {
    originalLoggerSupplier = HelpfulExecutors.createLogger;
  }

  @AfterEach
  void teardown() {
    HelpfulExecutors.createLogger = originalLoggerSupplier;
  }

  @Test
  void testLogUncaught() {
    Logger logger = mock(Logger.class);
    HelpfulExecutors.createLogger = x -> logger;
    AtomicBoolean hasRun = new AtomicBoolean(false);
    RuntimeException exception = new RuntimeException("Kaboom");
    Runnable runnable =
        HelpfulExecutors.logUncaught(
            () -> {
              hasRun.set(true);
              throw exception;
            });
    runnable.run();
    assertTrue(hasRun.get());
    verify(logger)
        .error("Uncaught exception in thread " + Thread.currentThread().getName(), exception);
  }

  @Test
  void testLogUncaught_nothingThrown() {
    Logger logger = mock(Logger.class);
    HelpfulExecutors.createLogger = x -> logger;
    AtomicBoolean hasRun = new AtomicBoolean(false);
    Runnable runnable =
        HelpfulExecutors.logUncaught(
            () -> {
              hasRun.set(true);
            });
    runnable.run();
    assertTrue(hasRun.get());
    verifyNoInteractions(logger);
  }

  @Test
  void testSingleThreadedExecutor() throws Exception {
    ExecutorService exec = HelpfulExecutors.newSingleThreadExecutor("test!!!");
    AtomicReference<String> seenName = new AtomicReference<>();
    AtomicBoolean seenIsDaemon = new AtomicBoolean(false);
    exec.submit(
        () -> {
          seenName.set(Thread.currentThread().getName());
          seenIsDaemon.set(Thread.currentThread().isDaemon());
        });
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, TimeUnit.SECONDS));
    assertEquals("test!!!", seenName.get());
    assertTrue(seenIsDaemon.get());
  }

  @Test
  void testScheduledSingleThreadedExecutor() throws Exception {
    ScheduledExecutorService exec = HelpfulExecutors.newSingleThreadedScheduledExecutor("test!!!");
    final ArrayBlockingQueue<String> seenNames = new ArrayBlockingQueue<>(10);
    final ArrayBlockingQueue<Boolean> seenIsDaemon = new ArrayBlockingQueue<>(10);
    final CountDownLatch latch = new CountDownLatch(3);
    exec.scheduleAtFixedRate(
        () -> {
          try {
            if (latch.getCount() > 0) {
              seenNames.put(Thread.currentThread().getName());
              seenIsDaemon.put(Thread.currentThread().isDaemon());
              latch.countDown();
            }
          } catch (InterruptedException e) {
            fail();
          }
        },
        0,
        1,
        TimeUnit.MILLISECONDS);
    latch.await();
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, TimeUnit.SECONDS));
    List<String> resultNames = new ArrayList<>();
    seenNames.drainTo(resultNames);
    List<Boolean> resultDaemons = new ArrayList<>();
    seenIsDaemon.drainTo(resultDaemons);
    assertEquals(Arrays.asList("test!!!", "test!!!", "test!!!"), resultNames);
    assertEquals(Arrays.asList(true, true, true), resultDaemons);
  }
}
