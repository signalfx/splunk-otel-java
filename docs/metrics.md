> The official Splunk documentation for this page is [Metrics and attributes of the Java agent](https://docs.splunk.com/Observability/gdi/get-data-in/application/java/configuration/java-otel-metrics-attributes.html). For instructions on how to contribute to the docs, see [CONTRIBUTING.md](../CONTRIBUTING.md#documentation).

# Metrics

> :construction: &nbsp;Status: Experimental - exported metric data and
> configuration properties may change.

The Splunk Distribution of OpenTelemetry Java agent gathers basic application metrics.
[Micrometer](https://micrometer.io/)
and [Micrometer SignalFx registry](https://micrometer.io/docs/registry/signalFx)
gather and export metrics to either [SignalFx SmartAgent](https://github.com/signalfx/signalfx-agent/)
or the [Splunk distribution of OpenTelemetry Collector](https://github.com/signalfx/splunk-otel-collector).

Because this feature is still experimental, metrics are not enabled by default. 
To enable metrics, add `-Dsplunk.metrics.enabled=true` to the JVM
arguments or set the environment variable `SPLUNK_METRICS_ENABLED` to `true`. 
For more information, please see the [advanced configuration](advanced-config.md#splunk-distribution-configuration)
for details.

## Default metric tags

The following dimensions are automatically added to all metrics exported by the agent:

| Tag name                 | Tag value |
| ------------------------ | --------- |
| `deployment.environment` | The value of the `deployment.environment` resource attribute, if present.
| `runtime`                | The value of the `process.runtime.name` resource attribute, e.g. `OpenJDK Runtime Environment`.
| `process.pid`            | The Java process identifier (PID).
| `service`                | The value of the `service.name` resource attribute.

## Manual instrumentation

The Splunk Distribution of OpenTelemetry Java agent detects if the instrumented application is using Micrometer and
injects a special `MeterRegistry` implementation that allows the agent to pick up custom user-defined meters, as long as
they're registered in the global `Metrics.globalRegistry` instance provided by the Micrometer library.

### Dependencies

You'll need to add a dependency on the `micrometer-core` library to be able to export custom metrics with the javaagent.

For Maven users:

```xml

<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
    <version>1.8.0</version>
</dependency>
```

For Gradle users:

```kotlin
implementation("io.micrometer:micrometer-core:1.8.0")
```

The agent supports all micrometer versions starting from 1.3.

### Adding custom metrics

You can use one of meter factory methods provided by the `Metrics` class, or use meter builders and refer to
the `Metrics.globalRegistry` directly:

```java
class MyClass {
  Counter myCounter = Metrics.counter("my_custom_counter");
  Timer myTimer = Timer.builder("my_custom_timer").register(Metrics.globalRegistry);

  int foo() {
    myCounter.increment();
    return myTimer.record(this::fooImpl);
  }

  private int fooImpl() {
    // ...
  }
}
```

For more details on using the Micrometer API please consult the [Micrometer docs](ohttps://micrometer.io/docs/concepts).

## Supported libraries

The following metrics are currently gathered by the agent:

| Library/Framework                                                    | Instrumentation name | Versions |
| -------------------------------------------------------------------- | -------------------- | -------- |
| [JVM metrics](#jvm)                                                  | `jvm-metrics`        | [Java runtimes version 8 and higher](../README.md#requirements)
| [Apache DBCP2 connection pool metrics](#connection-pool-metrics)     | `commons-dbcp2`      | 2.0 and higher
| [c3p0 connection pool metrics](#connection-pool-metrics)             | `c3p0`               | 0.9.5 and higher
| [HikariCP connection pool metrics](#connection-pool-metrics)         | `hikaricp`           | 3.0 and higher
| [Oracle Universal Connection Pool metrics](#connection-pool-metrics) | `oracle-ucp`         | 11.2.0.4 and higher
| [Tomcat JDBC connection pool metrics](#connection-pool-metrics)      | `tomcat-jdbc`        | 8.5 and higher
| [Vibur DBCP connection pool metrics](#connection-pool-metrics)       | `vibur-dbcp`         | 20.0 and higher
| [Tomcat thread pool metrics](#thread-pool-metrics)                   | `tomcat`             | 8.5 and higher
| [WebSphere Liberty web request thread pool](#thread-pool-metrics)    | `liberty`            | 20.0.0.12
| [WebLogic thread pools](#thread-pool-metrics)                        | `weblogic`           | 12.x and 14.x

### JVM

We use the [built-in Micrometer JVM metrics extension](https://micrometer.io/docs/ref/jvm)
to register JVM measurements.

#### Classloader metrics

| Metric name                    | Instrument   | Description |
| ------------------------------ | ------------ | ----------- |
| `runtime.jvm.classes.loaded`   | [Gauge][g]   | The number of loaded classes.
| `runtime.jvm.classes.unloaded` | [Counter][c] | The total number of unloaded classes since the process started.

#### GC metrics

| Metric name                            | Instrument   | Description |
| -------------------------------------- | ------------ | ----------- |
| `runtime.jvm.gc.concurrent.phase.time` | [Timer][t]   | Time spent in concurrent phase, in milliseconds.
| `runtime.jvm.gc.live.data.size`        | [Gauge][g]   | Size of long-lived heap memory pool after reclamation, in bytes.
| `runtime.jvm.gc.max.data.size`         | [Gauge][g]   | Max size of long-lived heap memory pool, in bytes.
| `runtime.jvm.gc.memory.allocated`      | [Counter][c] | Incremented for an increase in the size of the young heap memory pool after one garbage collection to before the next.
| `runtime.jvm.gc.memory.promoted`       | [Counter][c] | Count of positive increases in the size of the old generation memory pool before garbage collection to after garbage collection.
| `runtime.jvm.gc.pause`                 | [Timer][t]   | Time spent in garbage collection pause, in milliseconds.

#### Memory metrics

| Metric name                    | Instrument | Description |
| ------------------------------ | ---------- | ----------- |
| `runtime.jvm.memory.committed` | [Gauge][g] | The amount of memory that is committed for the Java virtual machine to use (in bytes).
| `runtime.jvm.memory.max`       | [Gauge][g] | The maximum amount of memory that can be used for memory management (in bytes).
| `runtime.jvm.memory.used`      | [Gauge][g] | The amount of used memory (in bytes).

All memory pool metrics have the following tags:

| Tag name | Tag value |
| -------- | --------- |
| `area`   | Either `heap` or `nonheap`.
| `id`     | Name of the memory pool, e.g. `Perm Gen`.

#### Thread metrics

| Metric name                  | Instrument | Description |
| ---------------------------- | ---------- | ----------- |
| `runtime.jvm.threads.daemon` | [Gauge][g] | The current number of live daemon threads.
| `runtime.jvm.threads.live`   | [Gauge][g] | The current number of live threads, including both daemon and non-daemon threads.
| `runtime.jvm.threads.peak`   | [Gauge][g] | The peak live thread count since the Java virtual machine started or the peak was reset.
| `runtime.jvm.threads.states` | [Gauge][g] | The current number of threads per `state` (metric tag).

### Connection pool metrics

Splunk Distribution of OpenTelemetry Java instruments several JDBC connection pool implementations:

* [Apache DBCP2](https://commons.apache.org/proper/commons-dbcp/)
* [c3p0](https://www.mchange.com/projects/c3p0/)
* [HikariCP](https://github.com/brettwooldridge/HikariCP)
* [Oracle Universal Connection Pool](https://docs.oracle.com/database/121/JJUCP/intro.htm#JJUCP8109)
* [Tomcat JDBC](https://tomcat.apache.org/tomcat-8.5-doc/jdbc-pool.html)
* [Vibur DBCP](https://github.com/vibur/vibur-dbcp)

Each of the supported connection pools reports a subset of the following metrics:

| Metric name                           | Instrument   | Description |
| ------------------------------------- | ------------ | ----------- |
| `db.pool.connections`                 | [Gauge][g]   | The number of open connections.
| `db.pool.connections.active`          | [Gauge][g]   | The number of open connections that are currently in use.
| `db.pool.connections.idle`            | [Gauge][g]   | The number of open connections that are currently idle.
| `db.pool.connections.idle.max`        | [Gauge][g]   | The maximum number of idle open connections allowed.
| `db.pool.connections.idle.min`        | [Gauge][g]   | The minimum number of idle open connections allowed.
| `db.pool.connections.max`             | [Gauge][g]   | The maximum number of open connections allowed.
| `db.pool.connections.pending_threads` | [Gauge][g]   | The number of threads that are currently waiting for an open connection.
| `db.pool.connections.timeouts`        | [Counter][c] | The number of connection timeouts that have happened since the application start.
| `db.pool.connections.create_time`     | [Timer][t]   | The time it took to create a new connection.
| `db.pool.connections.wait_time`       | [Timer][t]   | The time it took to get an open connection from the pool.
| `db.pool.connections.use_time`        | [Timer][t]   | The time between borrowing a connection and returning it to the pool.

All connection pool metrics have the following tags:

| Tag name    | Tag value |
| ----------- | --------- |
| `pool.name` | The name of the connection pool: Spring bean name if Spring is used, the JMX object name otherwise.
| `pool.type` | The type/implementation of the connection pool: e.g. `c3p0`, `dbcp2`, `hikari`, `oracle-ucp`, `tomcat-jdbc`, `vibur-dbcp`.

### Thread pool metrics

Splunk Distribution of OpenTelemetry Java instruments several thread pool implementations:

* [Tomcat connector thread pools](https://tomcat.apache.org/tomcat-8.5-doc/index.html)
* [WebSphere Liberty web request thread pool](https://www.ibm.com/docs/en/was-liberty/base?topic=10-threadpool-monitoring)
* [WebLogic thread pools](https://docs.oracle.com/en/middleware/standalone/weblogic-server/)

Each of the supported connection pools reports a subset of the following metrics:

| Metric name                | Instrument   | Description |
| -------------------------- | ------------ | ----------- |
| `executor.threads`         | [Gauge][g]   | The current number of threads in the pool.
| `executor.threads.active`  | [Gauge][g]   | The number of threads that are currently busy.
| `executor.threads.idle`    | [Gauge][g]   | The number of threads that are currently idle.
| `executor.threads.core`    | [Gauge][g]   | Core thread pool size - the number of threads that are always kept in the pool.
| `executor.threads.max`     | [Gauge][g]   | The maximum number of threads in the pool.
| `executor.tasks.submitted` | [Counter][c] | The total number of tasks that were submitted to this executor.
| `executor.tasks.completed` | [Counter][c] | The total number of tasks completed by this executor.

All thread pool metrics have the following tags:

| Tag name        | Tag value |
| --------------- | --------- |
| `executor.name` | The name of the thread pool.
| `executor.type` | The type/implementation of the thread pool: e.g. `tomcat`, `liberty`, `weblogic`.

[c]: https://micrometer.io/docs/concepts#_counters
[g]: https://micrometer.io/docs/concepts#_gauges
[t]: https://micrometer.io/docs/concepts#_timers
