# Advanced Configuration

The agent can be configured in the following ways:

* System property (example: `-Dotel.resource.attributes=service.name=my-java-app`)
* Environment variable (example: `export OTEL_RESOURCE_ATTRIBUTES=service.name=my-java-app`)

> System property values take priority over corresponding environment variables.

Below you will find all the configuration options supported by this distribution.

## Splunk distribution configuration

| System property                        | Environment variable                   | Default value           | Support      | Description |
| -------------------------------------- | -------------------------------------- | ----------------------- | ------------ | ----------- |
| `splunk.access.token`                  | `SPLUNK_ACCESS_TOKEN`                  | unset                   | Stable       | (Optional) Auth token allowing exporters to communicate directly with the Splunk cloud, passed as `X-SF-TOKEN` header. Currently, the [Jaeger trace exporter](#trace-exporters) as well as the [SignalFx metrics exporter](metrics.md) support this property.
| `splunk.metrics.enabled`               | `SPLUNK_METRICS_ENABLED`               | `false`                 | Experimental | Enables exporting metrics. See [this document](metrics.md) for more information.
| `splunk.metrics.endpoint`              | `SPLUNK_METRICS_ENDPOINT`              | `http://localhost:9943` | Experimental | The SignalFx metrics endpoint to connect to.
| `splunk.metrics.export.interval`       | `SPLUNK_METRICS_EXPORT_INTERVAL`       | `30000`                 | Experimental | The interval between pushing metrics, in milliseconds.
| `splunk.trace-response-header.enabled` | `SPLUNK_TRACE_RESPONSE_HEADER_ENABLED` | `true`                  | Stable       | Enables adding server trace information to HTTP response headers. See [this document](server-trace-info.md) for more information.

The SignalFx exporter can be configured to export metrics directly to Splunk ingest. To achieve that, you need to set
the `splunk.access.token` configuration property and set the `splunk.metrics.endpoint` to Splunk ingest URL. For
example:

```bash
export SPLUNK_ACCESS_TOKEN=my_splunk_token
export SPLUNK_METRICS_ENDPOINT=https://ingest.us0.signalfx.com
```

## Trace exporters

| System property                 | Environment variable              | Default value                    | Support | Description |
| ------------------------------- | --------------------------------- | -------------------------------- | ------- | ----------- |
| `otel.exporter.otlp.endpoint`   | `OTEL_EXPORTER_OTLP_ENDPOINT`     | `http://localhost:4317`          | Stable  | The OTLP endpoint to connect to.
| `otel.exporter.jaeger.endpoint` | `OTEL_EXPORTER_JAEGER_ENDPOINT`   | `http://localhost:9080/v1/trace` | Stable  | The Jaeger endpoint to connect to.
| `otel.traces.exporter`          | `OTEL_TRACES_EXPORTER`            | `otlp`                           | Stable  | Select the traces exporter to use. We recommend using either the OTLP exporter (`otlp`) or the Jaeger exporter (`jaeger-thrift-splunk`).

The Splunk Distribution of OpenTelemetry Java uses the OTLP traces exporter as the default setting. Please note that the
OTLP format is neither supported by the (now
deprecated) [SignalFx Smart Agent](https://github.com/signalfx/signalfx-agent) nor by the Splunk ingest. If you're still
using the Smart Agent, or if you wish to send traces directly to the Splunk Ingest, please use the Jaeger exporter. To
use the Jaeger exporter set the `otel.traces.exporter` configuration option to `jaeger-thrift-splunk`. For example:

```bash
export OTEL_TRACES_EXPORTER=jaeger-thrift-splunk
```

Jaeger exporter can be configured to export traces directly to the Splunk ingest. To achieve that, you need to set
the `splunk.access.token` configuration property and set the `otel.exporter.jaeger.endpoint` to Splunk ingest URL.

Example:

```bash
export SPLUNK_ACCESS_TOKEN=my_splunk_token
export OTEL_TRACES_EXPORTER=jaeger-thrift-splunk
export OTEL_EXPORTER_JAEGER_ENDPOINT=https://ingest.us0.signalfx.com/v2/trace
```

## Trace propagation configuration

| System property    | Environment variable | Default value                    | Support | Description |
| ------------------ | -------------------- | -------------------------------- | ------- | ----------- |
| `otel.propagators` | `OTEL_PROPAGATORS`   | `tracecontext,baggage`           | Stable  | A comma-separated list of propagators that will be used. You can find the list of supported propagators [here](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure#propagator).

If you wish to be compatible with older versions of the Splunk Distribution of OpenTelemetry Java (or the SignalFx
Tracing Java Agent) you can set the trace propagator to B3:

```bash
export OTEL_PROPAGATORS=b3multi
```

## Trace configuration

| System property                                                  | Environment variable                                             | Default value | Support | Description |
| ---------------------------------------------------------------- | ---------------------------------------------------------------- | ------------- | ------- | ----------- |
| `otel.instrumentation.common.peer-service-mapping`               | `OTEL_INSTRUMENTATION_COMMON_PEER_SERVICE_MAPPING`               | unset         | Stable  | Used to add a `peer.service` attribute by specifying a comma separated list of mapping from hostnames or IP addresses. <details><summary>Example</summary>If set to `1.2.3.4=cats-service,dogs-service.serverlessapis.com=dogs-api`, requests to `1.2.3.4` will have a `peer.service` attribute of `cats-service` and requests to `dogs-service.serverlessapis.com` will have one of `dogs-api`.</details>
| `otel.instrumentation.methods.include`                           | `OTEL_INSTRUMENTATION_METHODS_INCLUDE`                           | unset         | Stable  | Same as adding `@WithSpan` annotation functionality for the target method string. <details><summary>Format</summary>`my.package.MyClass1[method1,method2];my.package.MyClass2[method3]`</details>
| `otel.instrumentation.opentelemetry-annotations.exclude-methods` | `OTEL_INSTRUMENTATION_OPENTELEMETRY_ANNOTATIONS_EXCLUDE_METHODS` | unset         | Stable  | Suppress `@WithSpan` instrumentation for specific methods. <details><summary>Format</summary>`my.package.MyClass1[method1,method2];my.package.MyClass2[method3]`</details>
| `otel.resource.attributes`                                       | `OTEL_RESOURCE_ATTRIBUTES`                                       | unset         | Stable  | Comma-separated list of resource attributes added to every reported span. <details><summary>Example</summary>`key1=val1,key2=val2`</details>
| `otel.service.name`                                              | `OTEL_SERVICE_NAME`                                              | unset         | Stable  | Sets the value of the `service.name` resource attribute. If `service.name` is also set in `otel.resource.attributes`, this setting takes precedence.
| `otel.span.attribute.count.limit`                                | `OTEL_SPAN_ATTRIBUTE_COUNT_LIMIT`                                | unlimited     | Stable  | Maximum number of attributes per span.
| `otel.span.event.count.limit`                                    | `OTEL_SPAN_EVENT_COUNT_LIMIT`                                    | unlimited     | Stable  | Maximum number of events per span.
| `otel.span.link.count.limit`                                     | `OTEL_SPAN_LINK_COUNT_LIMIT`                                     | `1000`        | Stable  | Maximum number of links per span.

## Java agent configuration

| System property          | Environment variable     | Default value  | Support | Description |
| ------------------------ | ------------------------ | -------------- | ------- | ----------- |
| `otel.javaagent.enabled` | `OTEL_JAVAAGENT_ENABLED` | `true`         | Stable  | Globally enables javaagent auto-instrumentation.

## Other OpenTelemetry Java agent configuration

You can find all other Java agent configuration options
described [here](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/agent-config.md).
