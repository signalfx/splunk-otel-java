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

package com.splunk.opentelemetry.profiler.snapshot.simulation;

import io.opentelemetry.context.Context;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.UnaryOperator;

public class Background {

  /** Perform background task within the context of the same trace, waiting for the result. */
  public static <T> UnaryOperator<Message> task(Callable<T> task) {
    return message -> {
      var executor = Context.current().wrap(Executors.newSingleThreadExecutor());
      try {
        var future = executor.submit(task);
        try {
          future.get();
          return message;
        } catch (InterruptedException | ExecutionException e) {
          throw new RuntimeException(e);
        }
      } finally {
        executor.shutdownNow();
      }
    };
  }

  /** Perform background task within the context of the same trace. */
  public static UnaryOperator<Message> task(Runnable task) {
    CountDownLatch latch = new CountDownLatch(1);
    return message -> {
      var executor = Context.current().wrap(Executors.newSingleThreadExecutor());
      try {
        Runnable runnable =
            () -> {
              latch.countDown();
              task.run();
            };
        executor.submit(runnable);
        // wait until the task has started
        try {
          latch.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        return message;
      } finally {
        executor.shutdown();
      }
    };
  }
}
