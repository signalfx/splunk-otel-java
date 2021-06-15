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

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class WatchdogTimerTest {

  @Test
  void testSimpleWatchdog() throws Exception {
    CountDownLatch latch = new CountDownLatch(5);
    Runnable cb = latch::countDown;
    WatchdogTimer watchdog = new WatchdogTimer(Duration.ofMillis(10), cb);
    watchdog.start();
    assertTrue(latch.await(5, TimeUnit.SECONDS));
  }

  @Test
  void testCannotStartTwice() {
    Runnable cb = () -> {};
    WatchdogTimer watchdog = new WatchdogTimer(Duration.ofMillis(10), cb);
    watchdog.start();
    assertThrows(IllegalStateException.class, watchdog::start);
  }

  @Test
  void testMultipleResetAndStop() throws Exception {
    AtomicBoolean run = new AtomicBoolean(false);
    Runnable cb =
        () -> {
          run.set(true);
        };
    WatchdogTimer watchdog = new WatchdogTimer(Duration.ofMillis(50), cb);
    watchdog.start();
    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start < 250) {
      watchdog.reset();
      TimeUnit.MILLISECONDS.sleep(10);
    }
    watchdog.stop();
    TimeUnit.MILLISECONDS.sleep(150);
    assertFalse(run.get());
  }

  @Test
  void testResetBeforeStart() {
    Runnable cb = () -> {};
    WatchdogTimer watchdog = new WatchdogTimer(Duration.ofMillis(10), cb);
    assertThrows(IllegalStateException.class, watchdog::reset);
  }

  @Test
  void testStopBeforeStart() {
    Runnable cb = () -> {};
    WatchdogTimer watchdog = new WatchdogTimer(Duration.ofMillis(10), cb);
    assertThrows(IllegalStateException.class, watchdog::stop);
  }
}
