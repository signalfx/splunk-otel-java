# Advanced Configuration

The agent can be configured in the following ways:

* System property (example: `-Dotel.resource.attributes=service.name=my-java-app`)
* Environment variable (example: `export OTEL_RESOURCE_ATTRIBUTES=service.name=my-java-app`)

> System property values take priority over corresponding environment variables.

Below you will find all the configuration options supported by this distribution.

## Splunk distribution configuration

| System property                        | Environment variable                   | Default value                        | Purpose |
| -------------------------------------- | -------------------------------------- | ------------------------------------ | ------- |
| `splunk.trace-response-header.enabled` | `SPLUNK_TRACE_RESPONSE_HEADER_ENABLED` | `true`                               | Enables adding server trace information to HTTP response headers. See [this document](server-trace-info.md) for more information.
| `splunk.access.token`                  | `SPLUNK_ACCESS_TOKEN`                  | unset                                | (Optional) Auth token allowing exporters to communicate directly with the Splunk cloud, passed as `X-SF-TOKEN` header. Currently only the [Jaeger span exporter](#jaeger-exporter) and [SignalFx metrics exporter](metrics.md) support this property.
| `splunk.metrics.enabled`               | `SPLUNK_METRICS_ENABLED`               | `true`                               | Enables exporting metrics. See [this document](metrics.md) for more information.
| `splunk.metrics.endpoint`              | `SPLUNK_METRICS_ENDPOINT`              | `http://localhost:9080/v2/datapoint` | The SignalFx metrics endpoint to connect to.
| `splunk.metrics.export.interval`       | `SPLUNK_METRICS_EXPORT_INTERVAL`       | `10000`                              | The interval between pushing metrics, in milliseconds.

The SignalFx exporter can be configured to export metrics directly to Splunk ingest.
To achieve that, you need to set the `splunk.access.token` configuration property
and set the `splunk.metrics.endpoint` to Splunk ingest URL. For example:

```bash
export SPLUNK_ACCESS_TOKEN=my_splunk_token
export SPLUNK_METRICS_ENDPOINT=https://ingest.us0.signalfx.com
```

## Jaeger exporter

| System property                 | Environment variable              | Default value                    | Description |
| ------------------------------- | --------------------------------- | -------------------------------- | ----------- |
| `otel.traces.exporter`          | `OTEL_TRACES_EXPORTER`            | `jaeger-thrift-splunk`           | Select the span exporter to use.
| `otel.exporter.jaeger.endpoint` | `OTEL_EXPORTER_JAEGER_ENDPOINT`   | `http://localhost:9080/v1/trace` | The Jaeger endpoint to connect to.

The Jaeger exporter can be configured to export traces directly to Splunk ingest.
To achieve that, you need to set the `splunk.access.token` configuration property
and set the `otel.exporter.jaeger.endpoint` to Splunk ingest URL. For example:

```bash
export SPLUNK_ACCESS_TOKEN=my_splunk_token
export OTEL_EXPORTER_JAEGER_ENDPOINT=https://ingest.us0.signalfx.com/v2/trace
```

## Trace configuration

| System property                                                  | Environment variable                                             | Default value | Purpose |
| ---------------------------------------------------------------- | ---------------------------------------------------------------- | ------------- | ------- |
| `otel.span.attribute.count.limit`                                | `OTEL_SPAN_ATTRIBUTE_COUNT_LIMIT`                                | unlimited     | Maximum number of attributes per span.
| `otel.span.event.count.limit`                                    | `OTEL_SPAN_EVENT_COUNT_LIMIT`                                    | unlimited     | Maximum number of events per span.
| `otel.span.link.count.limit`                                     | `OTEL_SPAN_LINK_COUNT_LIMIT`                                     | `1000`        | Maximum number of links per span.
| `otel.resource.attributes`                                       | `OTEL_RESOURCE_ATTRIBUTES`                                       | unset         | Comma-separated list of resource attributes added to every reported span. <details><summary>Example</summary>`key1=val1,key2=val2`</details>
| `otel.instrumentation.common.peer-service-mapping`               | `OTEL_INSTRUMENTATION_COMMON_PEER_SERVICE_MAPPING`               | unset         | Used to add a `peer.service` attribute by specifying a comma separated list of mapping from hostnames or IP addresses. <details><summary>Example</summary>If set to `1.2.3.4=cats-service,dogs-service.serverlessapis.com=dogs-api`, requests to `1.2.3.4` will have a `peer.service` attribute of `cats-service` and requests to `dogs-service.serverlessapis.com` will have one of `dogs-api`.</details>
| `otel.instrumentation.methods.include`                           | `OTEL_INSTRUMENTATION_METHODS_INCLUDE`                           | unset         | Same as adding `@WithSpan` annotation functionality for the target method string. <details><summary>Format</summary>`my.package.MyClass1[method1,method2];my.package.MyClass2[method3]`</details>
| `otel.instrumentation.opentelemetry-annotations.exclude-methods` | `OTEL_INSTRUMENTATION_OPENTELEMETRY_ANNOTATIONS_EXCLUDE_METHODS` | unset         | Suppress `@WithSpan` instrumentation for specific methods. <details><summary>Format</summary>`my.package.MyClass1[method1,method2];my.package.MyClass2[method3]`</details>

## Java agent configuration

| System property          | Environment variable     | Default value  | Purpose                                          |
| ------------------------ | ------------------------ | -------------- | -------------------------------------------------|
| `otel.javaagent.enabled` | `OTEL_JAVAAGENT_ENABLED` | `true`         | Globally enables javaagent auto-instrumentation. |

## Deprecated configuration

These configuration options will be removed in the future; if you're still using one of them please migrate!

| Deprecated configuration option        | Replacement                            | Migration instructions |
| -------------------------------------- | -------------------------------------- | ---------------------- |
| `splunk.context.server-timing.enabled` | `splunk.trace-response-header.enabled` | The old property was renamed, the value and the way it works is exactly the same as it had been before.

## Other OpenTelemetry Java agent configuration

You can find all other Java agent configuration options
described [here](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/agent-config.md).
