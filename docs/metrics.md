> The official Splunk documentation for this page is [Metrics and attributes of the Java agent](https://help.splunk.com/en/splunk-observability-cloud/manage-data/available-data-sources/supported-integrations-in-splunk-observability-cloud/apm-instrumentation/instrument-a-java-application/metrics-and-attributes). For instructions on how to contribute to the docs, see [CONTRIBUTING.md](../CONTRIBUTING.md#documentation).

# Metrics and attributes

> :construction: &nbsp;Status: Experimental - exported metric data and
> configuration properties may change.

The Splunk Distribution of OpenTelemetry Java agent gathers basic application metrics.
Because these splunk-specific metrics are still experimental they are not enabled by default.

## Supported libraries

The following metrics are currently gathered by the agent:

| Library/Framework                                                    | Instrumentation name   | Versions |
| -------------------------------------------------------------------- |------------------------| -------- |
| [JVM metrics](#jvm)                                                  | `jvm-metrics-splunk`   | [Java runtimes version 8 and higher](../README.md#requirements)

### JVM

We use [OpenTelemetry JVM Metrics](https://opentelemetry.io/docs/specs/semconv/runtime/jvm-metrics/).
Besides OpenTelemetry JVM Metrics we include the following JVM metrics.

#### Memory profiler metrics

These metrics are enabled only when memory profiler is enabled.

| Metric name              | Instrument   | Description                                                              |
|--------------------------|--------------|--------------------------------------------------------------------------|
| `jvm.memory.allocated`   | [Counter][c] | Approximate sum of heap allocations.                                     |
| `jvm.gc.pause.count`     | [Counter][c] | Number of gc pauses. This metric will be removed in a future release.    |
| `jvm.gc.pause.totalTime` | [Counter][c] | Time spent in GC pause. This metric will be removed in a future release. |

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
