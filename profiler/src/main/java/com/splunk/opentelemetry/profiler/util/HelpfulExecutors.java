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

import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelpfulExecutors {

  @VisibleForTesting static Function<String, Logger> createLogger = LoggerFactory::getLogger;

  public static ScheduledExecutorService newSingleThreadedScheduledExecutor(String threadName) {
    return Executors.newSingleThreadScheduledExecutor(new HelpfulThreadFactory(threadName));
  }

  public static ExecutorService newSingleThreadExecutor(String name) {
    return Executors.newSingleThreadExecutor(new HelpfulThreadFactory(name));
  }

  public static Runnable logUncaught(Runnable delegate) {
    return () -> {
      try {
        delegate.run();
      } catch (Exception e) {
        String threadName = Thread.currentThread().getName();
        Logger logger = createLogger.apply(threadName);
        logger.error("Uncaught exception in thread " + threadName, e);
      }
    };
  }

  private static class HelpfulThreadFactory implements ThreadFactory {
    private final String threadName;

    public HelpfulThreadFactory(String threadName) {
      this.threadName = threadName;
    }

    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable, threadName);
      thread.setDaemon(true);
      return thread;
    }
  }
}
