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

import java.util.concurrent.Executor;
import java.util.function.Function;
import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.threads.ResizableExecutor;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;

/**
 * Used to do late-bound type-specific dispatch between one of two {@link TomcatExecutorMetrics}
 * implementations. The Tomcat AbstractEndpoint has a mutable executor instance, and thus cannot be
 * bound early. As such, this allows the binding to happen at metric gathering time. Mutations to
 * the endpoint's executor are honored.
 */
public class LateBindingTomcatThreadPoolMetrics {

  private final TomcatThreadPoolExecutorMetrics threadPoolExecutorSuppliers =
      TomcatThreadPoolExecutorMetrics.instance();
  private final TomcatResizeableExecutorMetrics resizeableExecutorSuppliers =
      TomcatResizeableExecutorMetrics.instance();
  private final AbstractEndpoint<?, ?> endpoint;

  public LateBindingTomcatThreadPoolMetrics(AbstractEndpoint<?, ?> endpoint) {
    this.endpoint = endpoint;
  }

  public Number getCurrentThreads() {
    return getMetric(
        endpoint.getExecutor(),
        threadPoolExecutorSuppliers::getCurrentThreads,
        resizeableExecutorSuppliers::getCurrentThreads);
  }

  public Number getActiveThreads() {
    return getMetric(
        endpoint.getExecutor(),
        threadPoolExecutorSuppliers::getActiveThreads,
        resizeableExecutorSuppliers::getActiveThreads);
  }

  public Number getCoreThreads() {
    return getMetric(
        endpoint.getExecutor(),
        threadPoolExecutorSuppliers::getCoreThreads,
        resizeableExecutorSuppliers::getCoreThreads);
  }

  public Number getMaxThreads() {
    return getMetric(
        endpoint.getExecutor(),
        threadPoolExecutorSuppliers::getMaxThreads,
        resizeableExecutorSuppliers::getMaxThreads);
  }

  public Number getSubmittedTasks() {
    return getMetric(
        endpoint.getExecutor(),
        threadPoolExecutorSuppliers::getSubmittedTasks,
        resizeableExecutorSuppliers::getSubmittedTasks);
  }

  public Number getCompletedTasks() {
    return getMetric(
        endpoint.getExecutor(),
        threadPoolExecutorSuppliers::getCompletedTasks,
        resizeableExecutorSuppliers::getCompletedTasks);
  }

  public Number getIdleThreads() {
    return getMetric(
        endpoint.getExecutor(),
        threadPoolExecutorSuppliers::getIdleThreads,
        resizeableExecutorSuppliers::getIdleThreads);
  }

  private Number getMetric(
      Executor executor,
      Function<ThreadPoolExecutor, Number> threadPoolMethod,
      Function<ResizableExecutor, Number> resizableExecMethod) {
    if (executor instanceof ThreadPoolExecutor) {
      return threadPoolMethod.apply((ThreadPoolExecutor) executor);
    }
    if (executor instanceof ResizableExecutor) {
      return resizableExecMethod.apply((ResizableExecutor) executor);
    }
    return Double.NaN;
  }
}
