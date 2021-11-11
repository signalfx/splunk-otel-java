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

import org.apache.tomcat.util.threads.ResizableExecutor;

/** Binds Tomcat's ResizeableExecutor to a set of common pool metrics. */
final class TomcatResizeableExecutorMetrics implements TomcatExecutorMetrics<ResizableExecutor> {

  private static final TomcatResizeableExecutorMetrics INSTANCE =
      new TomcatResizeableExecutorMetrics();

  public static TomcatResizeableExecutorMetrics instance() {
    return INSTANCE;
  }

  private TomcatResizeableExecutorMetrics() {}

  @Override
  public Number getCurrentThreads(ResizableExecutor executor) {
    return executor.getPoolSize();
  }

  @Override
  public Number getActiveThreads(ResizableExecutor executor) {
    return executor.getActiveCount();
  }

  @Override
  public Number getCoreThreads(ResizableExecutor executor) {
    return Double.NaN;
  }

  @Override
  public Number getMaxThreads(ResizableExecutor executor) {
    return executor.getMaxThreads();
  }

  @Override
  public Number getSubmittedTasks(ResizableExecutor executor) {
    return Double.NaN;
  }

  @Override
  public Number getCompletedTasks(ResizableExecutor executor) {
    return Double.NaN;
  }
}
