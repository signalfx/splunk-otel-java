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

import static java.util.logging.Level.SEVERE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RunnablesTest {

  Function<String, Logger> originalLoggerSupplier;

  @BeforeEach
  void setup() {
    originalLoggerSupplier = Runnables.createLogger;
  }

  @AfterEach
  void teardown() {
    Runnables.createLogger = originalLoggerSupplier;
  }

  @Test
  void testLogUncaught() {
    Logger logger = mock(Logger.class);
    Runnables.createLogger = x -> logger;
    AtomicBoolean hasRun = new AtomicBoolean(false);
    RuntimeException exception = new RuntimeException("Kaboom");
    Runnable runnable =
        Runnables.logUncaught(
            () -> {
              hasRun.set(true);
              throw exception;
            });
    runnable.run();
    assertTrue(hasRun.get());
    verify(logger)
        .log(SEVERE, "Uncaught exception in thread " + Thread.currentThread().getName(), exception);
  }

  @Test
  void testLogUncaught_nothingThrown() {
    Logger logger = mock(Logger.class);
    Runnables.createLogger = x -> logger;
    AtomicBoolean hasRun = new AtomicBoolean(false);
    Runnable runnable =
        Runnables.logUncaught(
            () -> {
              hasRun.set(true);
            });
    runnable.run();
    assertTrue(hasRun.get());
    verifyNoInteractions(logger);
  }
}
