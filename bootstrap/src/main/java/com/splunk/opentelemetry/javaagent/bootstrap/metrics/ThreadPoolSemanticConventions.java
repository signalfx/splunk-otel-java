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

package com.splunk.opentelemetry.javaagent.bootstrap.metrics;

import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.CounterSemanticConvention.counter;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.GaugeSemanticConvention.gauge;
import static io.micrometer.core.instrument.binder.BaseUnits.TASKS;
import static io.micrometer.core.instrument.binder.BaseUnits.THREADS;

public final class ThreadPoolSemanticConventions {

  /** The current number of threads in the pool. */
  public static final GaugeSemanticConvention THREADS_CURRENT = gauge("executor.threads", THREADS);
  /** The number of threads that are currently busy. */
  public static final GaugeSemanticConvention THREADS_ACTIVE =
      gauge("executor.threads.active", THREADS);
  /** The number of threads that are currently idle. */
  public static final GaugeSemanticConvention THREADS_IDLE =
      gauge("executor.threads.idle", THREADS);
  /** Core thread pool size - the number of threads that are always kept in the pool. */
  public static final GaugeSemanticConvention THREADS_CORE =
      gauge("executor.threads.core", THREADS);
  /** The maximum number of threads in the pool. */
  public static final GaugeSemanticConvention THREADS_MAX = gauge("executor.threads.max", THREADS);
  /** The total number of tasks that were submitted to this executor. */
  public static final CounterSemanticConvention TASKS_SUBMITTED =
      counter("executor.tasks.submitted", TASKS);
  /** The total number of tasks completed by this executor. */
  public static final CounterSemanticConvention TASKS_COMPLETED =
      counter("executor.tasks.completed", TASKS);

  /** The name of the thread pool, as named by the instrumented application developer. */
  public static final String EXECUTOR_NAME = "executor.name";
  /**
   * Type/implementation of the DB connection pool.
   *
   * <p>In the future, once we start using OTel metrics API, will be replaced by {@code
   * InstrumentationLibrary}.
   */
  public static final String EXECUTOR_TYPE = "executor.type";

  private ThreadPoolSemanticConventions() {}
}
