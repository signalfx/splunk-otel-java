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

/**
 * This is a common interface that allows runtime binding from a concrete Tomcat thread pool type to
 * set of common metric values.
 *
 * @param <T> The type of Executor implementation
 */
public interface TomcatExecutorMetrics<T extends Executor> {

  Number getCurrentThreads(T executor);

  Number getActiveThreads(T executor);

  default Number getIdleThreads(T executor) {
    return getCurrentThreads(executor).intValue() - getActiveThreads(executor).intValue();
  }

  Number getCoreThreads(T executor);

  Number getMaxThreads(T executor);

  Number getSubmittedTasks(T executor);

  Number getCompletedTasks(T executor);
}
