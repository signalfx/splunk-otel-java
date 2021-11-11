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

package com.splunk.opentelemetry.instrumentation.tomcat.metrics;

import org.apache.tomcat.util.threads.ThreadPoolExecutor;

/**
 * Binds Tomcat's ThreadPoolExecutor (not to be confused with j.u.c.ThreadPoolExecutor) to a set of
 * common metrics.
 */
final class TomcatThreadPoolExecutorMetrics implements TomcatExecutorMetrics<ThreadPoolExecutor> {

  private static final TomcatThreadPoolExecutorMetrics INSTANCE =
      new TomcatThreadPoolExecutorMetrics();

  public static TomcatThreadPoolExecutorMetrics instance() {
    return INSTANCE;
  }

  @Override
  public Number getCurrentThreads(ThreadPoolExecutor executor) {
    return executor.getPoolSize();
  }

  @Override
  public Number getActiveThreads(ThreadPoolExecutor executor) {
    return executor.getActiveCount();
  }

  @Override
  public Number getCoreThreads(ThreadPoolExecutor executor) {
    return executor.getCorePoolSize();
  }

  @Override
  public Number getMaxThreads(ThreadPoolExecutor executor) {
    return executor.getMaximumPoolSize();
  }

  @Override
  public Number getSubmittedTasks(ThreadPoolExecutor executor) {
    return executor.getSubmittedCount();
  }

  @Override
  public Number getCompletedTasks(ThreadPoolExecutor executor) {
    return executor.getCompletedTaskCount();
  }
}
