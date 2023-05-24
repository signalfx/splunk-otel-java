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

package com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer;

import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.CounterSemanticConvention.counter;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.GaugeSemanticConvention.gauge;
import static com.splunk.opentelemetry.javaagent.bootstrap.metrics.micrometer.TimerSemanticConvention.timer;
import static io.micrometer.core.instrument.binder.BaseUnits.CONNECTIONS;
import static io.micrometer.core.instrument.binder.BaseUnits.THREADS;

public final class ConnectionPoolSemanticConventions {

  /** The number of open connections. */
  public static final GaugeSemanticConvention CONNECTIONS_TOTAL =
      gauge("db.pool.connections", CONNECTIONS);

  /** The number of open connections that are currently in use. */
  public static final GaugeSemanticConvention CONNECTIONS_ACTIVE =
      gauge("db.pool.connections.active", CONNECTIONS);

  /** The number of open connections that are currently idle. */
  public static final GaugeSemanticConvention CONNECTIONS_IDLE =
      gauge("db.pool.connections.idle", CONNECTIONS);

  /** The minimum number of idle open connections allowed. */
  public static final GaugeSemanticConvention CONNECTIONS_IDLE_MIN =
      gauge("db.pool.connections.idle.min", CONNECTIONS);

  /** The maximum number of idle open connections allowed. */
  public static final GaugeSemanticConvention CONNECTIONS_IDLE_MAX =
      gauge("db.pool.connections.idle.max", CONNECTIONS);

  /** The maximum number of open connections allowed. */
  public static final GaugeSemanticConvention CONNECTIONS_MAX =
      gauge("db.pool.connections.max", CONNECTIONS);

  /** The number of threads that are currently waiting for an open connection. */
  public static final GaugeSemanticConvention CONNECTIONS_PENDING_THREADS =
      gauge("db.pool.connections.pending_threads", THREADS);

  /** The number of connection timeouts that have happened since the application start. */
  public static final CounterSemanticConvention CONNECTIONS_TIMEOUTS =
      counter("db.pool.connections.timeouts", "timeouts");

  /** The time it took to create a new connection. */
  public static final TimerSemanticConvention CONNECTIONS_CREATE_TIME =
      timer("db.pool.connections.create_time");

  /** The time it took to get an open connection from the pool. */
  public static final TimerSemanticConvention CONNECTIONS_WAIT_TIME =
      timer("db.pool.connections.wait_time");

  /** The time between borrowing a connection and returning it to the pool. */
  public static final TimerSemanticConvention CONNECTIONS_USE_TIME =
      timer("db.pool.connections.use_time");

  /** The name of the DB connection pool, as named by the instrumented application developer. */
  public static final String POOL_NAME = "pool.name";

  /**
   * Type/implementation of the DB connection pool.
   *
   * <p>In the future, once we start using OTel metrics API, will be replaced by {@code
   * InstrumentationLibrary}.
   */
  public static final String POOL_TYPE = "pool.type";

  private ConnectionPoolSemanticConventions() {}
}
