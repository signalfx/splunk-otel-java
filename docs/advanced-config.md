# Advanced Configuration

The agent can be configured in the following ways:

* System property (example: `-Dotel.resource.attributes=service.name=my-java-app`)
* Environment variable (example: `export OTEL_RESOURCE_ATTRIBUTES=service.name=my-java-app`)

> System property values take priority over corresponding environment variables.

Below you will find all the configuration options supported by this distribution.

## Splunk distribution configuration

| System property                        | Environment variable                   | Default value  | Purpose |
| -------------------------------------- | -------------------------------------- | -------------- | ------- |
| `splunk.context.server-timing.enabled` | `SPLUNK_CONTEXT_SERVER_TIMING_ENABLED` | `false`        | Enables adding `Server-Timing` header to HTTP responses. See [this document](server-timing.md) for more information.
| `splunk.access.token`                  | `SPLUNK_ACCESS_TOKEN`                  | unset          | (Optional) Auth token allowing exporters to communicate directly with the Splunk cloud, passed as `X-SF-TOKEN` header. Currently only the [Jaeger exporter](#jaeger-exporter) supports this property.

## Jaeger exporter

| System property                 | Environment variable              | Default value                    | Description |
| ------------------------------- | --------------------------------- | -------------------------------- | ----------- |
| `otel.traces.exporter`          | `OTEL_TRACES_EXPORTER`            | `jaeger-thrift-splunk`           | Select the span exporter to use.
| `otel.exporter.jaeger.endpoint` | `OTEL_EXPORTER_JAEGER_ENDPOINT`   | `http://localhost:9080/v1/trace` | The Jaeger endpoint to connect to.

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

## Other OpenTelemetry Java agent configuration

You can find all other Java agent configuration options
described [here](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/agent-config.md).
