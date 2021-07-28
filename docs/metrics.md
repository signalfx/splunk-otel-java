# Metrics

> :construction: &nbsp;Status: Experimental - exported metric data and
> configuration properties may change.

The Splunk Distribution of OpenTelemetry Java agent gathers basic application metrics.
[Micrometer](https://micrometer.io/)
and [Micrometer SignalFx registry](https://micrometer.io/docs/registry/signalFx)
gather and export metrics to either [SignalFx SmartAgent](https://github.com/signalfx/signalfx-agent/)
or the [Splunk distribution of OpenTelemetry Collector](https://github.com/signalfx/splunk-otel-collector).

## Default metric tags

The following dimensions are automatically added to all metrics exported by the agent:

| Tag name                 | Tag value |
| ------------------------ | --------- |
| `deployment.environment` | The value of the `deployment.environment` resource attribute, if present.
| `runtime`                | The value of the `process.runtime.name` resource attribute, e.g. `OpenJDK Runtime Environment`.
| `process.pid`            | The Java process identifier (PID).
| `service`                | The value of the `service.name` resource attribute.

## Supported libraries

The following metrics are currently gathered by the agent:

| Library/Framework                                                | Instrumentation name | Versions |
| ---------------------------------------------------------------- | -------------------- | -------- |
| [JVM metrics](#jvm)                                              | `jvm-metrics`        | [Java runtimes version 8 and higher](../README.md#supported-java-versions)
| [Apache DBCP2 connection pool metrics](#connection-pool-metrics) | `commons-dbcp2`      | 2.0 and higher
| [c3p0 connection pool metrics](#connection-pool-metrics)         | `c3p0`               | 0.9.5 and higher
| [HikariCP connection pool metrics](#connection-pool-metrics)     | `hikaricp`           | 3.0 and higher
| [Tomcat JDBC connection pool metrics](#connection-pool-metrics)  | `tomcat-jdbc`        | 8.5 and higher

### JVM

We use the [built-in Micrometer JVM metrics extension](https://micrometer.io/docs/ref/jvm)
to register JVM measurements.

#### Classloader metrics

| Metric name                    | Description |
| ------------------------------ | ----------- |
| `runtime.jvm.classes.loaded`   | The number of loaded classes.
| `runtime.jvm.classes.unloaded` | The total number of unloaded classes since process start.

#### GC metrics

| Metric name                            | Description |
| -------------------------------------- | ----------- |
| `runtime.jvm.gc.concurrent.phase.time` | Time spent in concurrent phase.
| `runtime.jvm.gc.live.data.size`        | Size of long-lived heap memory pool after reclamation.
| `runtime.jvm.gc.max.data.size`         | Max size of long-lived heap memory pool.
| `runtime.jvm.gc.memory.allocated`      | Incremented for an increase in the size of the (young) heap memory pool after one GC to before the next.
| `runtime.jvm.gc.memory.promoted`       | Count of positive increases in the size of the old generation memory pool before GC to after GC.
| `runtime.jvm.gc.pause`                 | Time spent in GC pause.

#### Memory metrics

| Metric name                    | Description |
| ------------------------------ | ----------- |
| `runtime.jvm.memory.committed` | The amount of memory in bytes that is committed for the Java virtual machine to use.
| `runtime.jvm.memory.max`       | The maximum amount of memory in bytes that can be used for memory management.
| `runtime.jvm.memory.used`      | The amount of used memory.

All memory pool metrics have the following tags:

| Tag name | Tag value |
| -------- | --------- |
| `area`   | Either `heap` or `nonheap`.
| `id`     | Name of the memory pool, e.g. `Perm Gen`.

#### Thread metrics

| Metric name                  | Description |
| ---------------------------- | ----------- |
| `runtime.jvm.threads.daemon` | The current number of live daemon threads.
| `runtime.jvm.threads.live`   | The current number of live threads including both daemon and non-daemon threads.
| `runtime.jvm.threads.peak`   | The peak live thread count since the Java virtual machine started or peak was reset.
| `runtime.jvm.threads.states` | The current number of threads per `state` (metric tag).

### Connection pool metrics

Splunk Distribution of OpenTelemetry Java instruments several JDBC connection pool implementations:

* [Apache DBCP2](https://commons.apache.org/proper/commons-dbcp/)
* [c3p0](https://www.mchange.com/projects/c3p0/)
* [HikariCP](https://github.com/brettwooldridge/HikariCP)
* [Tomcat JDBC](https://tomcat.apache.org/tomcat-8.5-doc/jdbc-pool.html)

Each of the supported connection pools reports a subset of the following metrics:

| Metric name                           | Description |
| ------------------------------------- | ----------- |
| `db.pool.connections`                 | The number of open connections.
| `db.pool.connections.active`          | The number of open connections that are currently in use.
| `db.pool.connections.idle`            | The number of open connections that are currently idle.
| `db.pool.connections.idle.max`        | The maximum number of idle open connections allowed.
| `db.pool.connections.idle.min`        | The minimum number of idle open connections allowed.
| `db.pool.connections.max`             | The maximum number of open connections allowed.
| `db.pool.connections.pending_threads` | The number of threads that are currently waiting for an open connection.
| `db.pool.connections.timeouts`        | The number of connection timeouts that have happened since the application start.
| `db.pool.connections.create_time`     | The time it took to create a new connection.
| `db.pool.connections.wait_time`       | The time it took to get an open connection from the pool.
| `db.pool.connections.use_time`        | The time between borrowing a connection and returning it to the pool.

All connection pool metrics have the following tags:

| Tag name    | Tag value |
| ----------- | --------- |
| `pool.name` | The name of the connection pool: Spring bean name if Spring is used, the JMX object name otherwise.
| `pool.type` | The type/implementation of the connection pool: e.g. `c3p0`, `dbcp2`, `hikari`, `tomcat-jdbc`.
