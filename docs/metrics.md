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

## Custom JMX metrics in declarative configuration

Define inline JMX mapping rules under `instrumentation/development.java.jmx.rules` in the
file selected by `otel.config.file`:

```yaml
file_format: "1.2"
meter_provider:
  readers:
    - periodic:
        exporter:
          otlp_http:

instrumentation/development:
  java:
    jmx:
      discovery:
        delay: 5000
      metrics:
        included: [petclinic.*]
      rules:
        - bean: petclinic:type=CallMonitor
          mapping:
            CallCount:
              metric: petclinic.repository.call.count
              type: gauge
              unit: "{call}"
              desc: Completed repository calls since the last reset.
            CallTime:
              metric: petclinic.repository.call.duration.average
              type: gauge
              sourceUnit: ms
              unit: s
              desc: Average repository call duration since the last reset.
```

Rules use the [upstream JMX mapping syntax](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/jmx-metrics/README.md).
The agent collects from MBeans in the current JVM and exports through the configured
`meter_provider` readers. No remote JMX connection is required.

The JMX node accepts the same settings and defaults as the upstream agent:

* `discovery.delay`: MBean discovery delay in milliseconds (default: `60000`).
* `config`: a list of paths to external JMX mapping files. Invalid files are logged and skipped.
* `metrics.included` / `metrics.excluded`: filters applied to all JMX metric names.
* `metrics.experimental.included`: opt in to experimental built-in metrics by name.

Stable built-in metrics are enabled by default, except bundled JVM rules that overlap with
runtime telemetry. Inline and external custom rules do not require experimental opt-in.
The inclusion filter in the example restricts collection to the Petclinic metric names.
An empty JMX node selects upstream defaults. Invalid inline rules are logged and skipped.

The `jmx` enable/disable selectors in `distribution.javaagent.instrumentation` determine whether
collection is allowed. For example, `disabled: [jmx]` disables collection. Setting
`default_enabled: false` also disables JMX unless `enabled: [jmx]` is configured.

Environment and system-property configuration is supported through the upstream configuration
bridge, including `OTEL_JMX_CONFIG`, `OTEL_JMX_DISCOVERY_DELAY`, and the JMX metric filters.
`OTEL_INSTRUMENTATION_JMX_ENABLED=false` disables collection. The common instrumentation default
applies when JMX enablement is not explicitly configured.

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
