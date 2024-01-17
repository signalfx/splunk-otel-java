> The official Splunk documentation for this page is [Metrics and attributes of the Java agent](https://quickdraw.splunk.com/redirect/?product=Observability&version=current&location=java.gdi.metrics). For instructions on how to contribute to the docs, see [CONTRIBUTING.md](../CONTRIBUTING.md#documentation).

# Metrics and attributes

> :construction: &nbsp;Status: Experimental - exported metric data and
> configuration properties may change.

The Splunk Distribution of OpenTelemetry Java agent gathers basic application metrics.
Because these splunk-specific metrics are still experimental they are not enabled by default.
To enable metrics, add `-Dsplunk.metrics.enabled=true` to the JVM
arguments or set the environment variable `SPLUNK_METRICS_ENABLED` to `true`.
For more information, please see the [advanced configuration](advanced-config.md#splunk-distribution-configuration)
for details.

## Supported libraries

The following metrics are currently gathered by the agent:

| Library/Framework                                                    | Instrumentation name   | Versions |
| -------------------------------------------------------------------- |------------------------| -------- |
| [JVM metrics](#jvm)                                                  | `jvm-metrics-splunk`   | [Java runtimes version 8 and higher](../README.md#requirements)
| [Tomcat thread pool metrics](#thread-pool-metrics)                   | `tomcat`               | 8.5 and higher
| [WebSphere Liberty web request thread pool](#thread-pool-metrics)    | `liberty`              | 20.0.0.12
| [WebLogic thread pools](#thread-pool-metrics)                        | `weblogic`             | 12.x and 14.x

### JVM

We use [OpenTelemetry JVM Metrics](https://opentelemetry.io/docs/specs/semconv/runtime/jvm-metrics/).
Besides OpenTelemetry JVM Metrics we include the following JVM metrics.

#### Memory profiler metrics

These metrics are enabled only when memory profiler is enabled.

| Metric name                            | Instrument   | Description                                                      |
|----------------------------------------|--------------|------------------------------------------------------------------|
| `process.runtime.jvm.memory.allocated` | [Counter][c] | Approximate sum of heap allocations.                             |
| `process.runtime.jvm.memory.reclaimed` | [Counter][c] | Sum of heap size differences before and after gc.                |
| `runtime.jvm.gc.pause.count`           | [Counter][c] | Number of gc pauses.                                             |
| `runtime.jvm.gc.pause.totalTime`       | [Counter][c] | Time spent in GC pause.                                          |
| `runtime.jvm.gc.live.data.size`        | [Gauge][g]   | Size of long-lived heap memory pool after reclamation, in bytes. |

#### Thread metrics

| Metric name                  | Instrument | Description |
| ---------------------------- | ---------- | ----------- |
| `runtime.jvm.threads.states` | [Gauge][g] | The current number of threads per `state` (metric tag).

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

All thread pool metrics have the following attributes:

| Attribute name  | Attribute value                                                                   |
|-----------------|-----------------------------------------------------------------------------------|
| `executor.name` | The name of the thread pool.                                                      |
| `executor.type` | The type/implementation of the thread pool: e.g. `tomcat`, `liberty`, `weblogic`. |

[c]: https://opentelemetry.io/docs/specs/otel/metrics/api/#counter
[g]: https://opentelemetry.io/docs/specs/otel/metrics/api/#gauge

## Webengine Attributes

> :construction: &nbsp;Status: Experimental

The Splunk Distribution of OpenTelemetry Java captures information about the application server that is being used and
adds the following attributes to `SERVER` spans:

| Span attribute       | Example     | Description |
| -------------------- | ----------- | ----------- |
| `webengine.name`    | `tomcat`    | The name of the application server.
| `webengine.version` | `7.0.107.0` | The version of the application server.

All application servers
from [this list](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/supported-libraries.md#application-servers)
are supported.
