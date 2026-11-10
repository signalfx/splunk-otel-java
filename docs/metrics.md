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

Define inline JMX mapping rules under `distribution.splunk.java.jmx.rules` in the
file selected by `otel.config.file`:

```yaml
file_format: "1.2"
meter_provider:
  readers:
    - periodic:
        exporter:
          otlp_http:

distribution:
  splunk:
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

The distribution JMX node accepts the same settings and defaults as the upstream agent:

* `discovery.delay`: MBean discovery delay in milliseconds (default: `60000`).
* `config`: a list of paths to external JMX mapping files. Invalid files are logged and skipped.
* `metrics.included` / `metrics.excluded`: filters applied to all JMX metric names.
* `metrics.experimental.included`: opt in to experimental built-in metrics by name.

Stable built-in metrics are enabled by default, except bundled JVM rules that overlap with
runtime telemetry. Inline and external custom rules do not require experimental opt-in.
The inclusion filter in the example restricts collection to the Petclinic metric names.
An empty distribution JMX node selects upstream defaults.

When `distribution.splunk.java` contains `jmx`, the agent takes ownership of JMX collection.
If `instrumentation/development.java.jmx` is also present, its settings are merged into the
distribution node while the instrumentation node is retained. Distribution values take precedence;
nested mappings are merged and configured lists replace the corresponding upstream lists.
The takeover is logged at INFO level, and upstream JMX startup is disabled automatically to
prevent duplicate collection. Without a distribution JMX node, upstream behavior is unchanged.

Explicit `jmx` enable/disable selectors in `distribution.javaagent.instrumentation` are respected
before the takeover. They determine whether the custom collection starts; the automatically
injected upstream disablement does not disable it. Invalid inline rules are logged and skipped.

JMX configuration is customized in two stages:

* `JmxUpstreamDisablingCustomizerProvider` runs at order `-1`. It saves the original enablement
  decision and disables upstream JMX before `JavaagentDistributionAccessCustomizerProvider`
  snapshots instrumentation enablement at order `0`. Both JMX nodes remain available to other
  model customizers.
* `JmxMetricsConfigurationCustomizerProvider` runs at order `Integer.MAX_VALUE - 1`. It merges
  the customized JMX settings while retaining the instrumentation node, and captures the merged
  settings for `JmxMetricsInitializer`. Order `Integer.MAX_VALUE` is reserved for final model
  capture, including the OpAMP configuration interceptor.

Running the entire customization late would leave upstream JMX enabled: changing the model does
not update its earlier enablement snapshot. Saving the original decision also ensures that our
injected disablement does not turn off distribution-owned collection.

The distribution JMX node must exist before the early stage; enablement is evaluated then using
the configured selectors and their defaults. Customizers that create this node or change enablement
must run before order `-1`. Later customizers can change the JMX mapping rules and collection
settings before the merge. A node first added after the early stage does not start custom
collection, because upstream may already be enabled.

When OpAMP remote configuration is enabled, updates under `distribution.splunk.java.jmx` are
applied by `JmxMetricInsightSupervisor`, which also owns the startup collection. Each received JMX
node is a complete replacement for the current JMX configuration. Omitted settings use their
defaults, and an empty node resets collection to defaults, clearing previous custom rules and
filters. A message without this JMX node leaves collection unchanged, and an identical
configuration does not restart it. New rules are validated before stopping the previous
collection; invalid inline rules are reported as a failed remote update.

Remote JMX updates require distribution-owned JMX to be enabled at startup, for example with
`distribution.splunk.java.jmx: {}`. Startup disablement remains effective, and an already-running
upstream collector cannot be taken over remotely.

This distribution-specific startup wiring and upstream configuration adapter are temporary and
isolated in `custom`'s `com.splunk.opentelemetry.jmx` package for removal when upstream supports
embedded rules.

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
