> The official Splunk documentation for this page is [Configure the Java agent](https://quickdraw.splunk.com/redirect/?product=Observability&version=current&location=java.gdi.settings). For instructions on how to contribute to the docs, see [CONTRIBUTING.md](../CONTRIBUTING.md#documentation).

# Advanced configuration

The agent can be configured in the following ways:

* System property (example: `-Dotel.resource.attributes=service.name=my-java-app`)
* Environment variable (example: `export OTEL_RESOURCE_ATTRIBUTES=service.name=my-java-app`)

> System property values take priority over corresponding environment variables.

Below you will find all the configuration options supported by this distribution.

## Splunk configuration

| System property                         | Environment variable                    | Default value | Support      | Description                                                                                                                                                                                                                                                                                                                                                      |
|-----------------------------------------|-----------------------------------------|---------------|--------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `splunk.access.token`                   | `SPLUNK_ACCESS_TOKEN`                   | unset         | Stable       | (Optional) Auth token allowing exporters to communicate directly with the Splunk cloud, passed as `X-SF-TOKEN` header.                                                                                                                                                                                                                                           |
| `splunk.realm`                          | `SPLUNK_REALM`                          | `none`        | Stable       | The Splunk Observability Cloud realm where the telemetry should be sent to. For example, `us0` or `us1`. Defaults to `none`, which means that data goes to a Splunk OpenTelemetry Collector deployed on `localhost`.                                                                                                                                             |
| `splunk.metrics.force_full_commandline` | `SPLUNK_METRICS_FORCE_FULL_COMMANDLINE` | `false`       | Experimental | Adds the full command line as a resource attribute for all metrics. If false, commands longer than 255 characters are truncated.                                                                                                                                                                                                                                 |
| `splunk.trace-response-header.enabled`  | `SPLUNK_TRACE_RESPONSE_HEADER_ENABLED`  | `true`        | Stable       | Enables adding server trace information to HTTP response headers. See [this document](https://help.splunk.com/en/splunk-observability-cloud/manage-data/available-data-sources/supported-integrations-in-splunk-observability-cloud/apm-instrumentation/instrument-a-java-application/configure-the-java-agent#server-trace-information-0) for more information. |

## Trace exporters

| System property                 | Environment variable            | Default value                    | Support | Description                                                                               |
|---------------------------------|---------------------------------|----------------------------------|---------|-------------------------------------------------------------------------------------------|
| `otel.exporter.otlp.endpoint`   | `OTEL_EXPORTER_OTLP_ENDPOINT`   | `http://localhost:4317`          | Stable  | The OTLP endpoint to connect to. Setting this will override the `splunk.realm` property.  |
| `otel.traces.exporter`          | `OTEL_TRACES_EXPORTER`          | `otlp`                           | Stable  | Select the traces exporter to use. We recommend using the default OTLP exporter (`otlp`)  |

The Splunk Distribution of OpenTelemetry Java uses the OTLP traces exporter as the default setting.

## Trace propagation configuration

| System property    | Environment variable | Default value          | Support | Description                                                                                                                                                                                                             |
|--------------------|----------------------|------------------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `otel.propagators` | `OTEL_PROPAGATORS`   | `tracecontext,baggage` | Stable  | A comma-separated list of propagators that will be used. You can find the list of supported propagators [here](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure#propagator). |

If you wish to be compatible with older versions of the Splunk Distribution of OpenTelemetry Java (or the SignalFx
Tracing Java Agent) you can set the trace propagator to B3:

```bash
export OTEL_PROPAGATORS=b3multi
```

## Trace configuration

| System property                                                  | Environment variable                                             | Default value | Support | Description                                                                                                                                                                                                                                                                                                                                                                                                |
|------------------------------------------------------------------|------------------------------------------------------------------|---------------|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `otel.instrumentation.common.peer-service-mapping`               | `OTEL_INSTRUMENTATION_COMMON_PEER_SERVICE_MAPPING`               | unset         | Stable  | Used to add a `peer.service` attribute by specifying a comma separated list of mapping from hostnames or IP addresses. <details><summary>Example</summary>If set to `1.2.3.4=cats-service,dogs-service.serverlessapis.com=dogs-api`, requests to `1.2.3.4` will have a `peer.service` attribute of `cats-service` and requests to `dogs-service.serverlessapis.com` will have one of `dogs-api`.</details> |
| `otel.instrumentation.methods.include`                           | `OTEL_INSTRUMENTATION_METHODS_INCLUDE`                           | unset         | Stable  | Same as adding `@WithSpan` annotation functionality for the target method string. <details><summary>Format</summary>`my.package.MyClass1[method1,method2];my.package.MyClass2[method3]`</details>                                                                                                                                                                                                          |
| `otel.instrumentation.opentelemetry-annotations.exclude-methods` | `OTEL_INSTRUMENTATION_OPENTELEMETRY_ANNOTATIONS_EXCLUDE_METHODS` | unset         | Stable  | Suppress `@WithSpan` instrumentation for specific methods. <details><summary>Format</summary>`my.package.MyClass1[method1,method2];my.package.MyClass2[method3]`</details>                                                                                                                                                                                                                                 |
| `otel.resource.attributes`                                       | `OTEL_RESOURCE_ATTRIBUTES`                                       | unset         | Stable  | Comma-separated list of resource attributes added to every reported span. <details><summary>Example</summary>`key1=val1,key2=val2`</details>                                                                                                                                                                                                                                                               |
| `otel.service.name`                                              | `OTEL_SERVICE_NAME`                                              | unset         | Stable  | Sets the value of the `service.name` resource attribute. If `service.name` is also set in `otel.resource.attributes`, this setting takes precedence.                                                                                                                                                                                                                                                       |
| `otel.span.attribute.count.limit`                                | `OTEL_SPAN_ATTRIBUTE_COUNT_LIMIT`                                | unlimited     | Stable  | Maximum number of attributes per span.                                                                                                                                                                                                                                                                                                                                                                     |
| `otel.span.event.count.limit`                                    | `OTEL_SPAN_EVENT_COUNT_LIMIT`                                    | unlimited     | Stable  | Maximum number of events per span.                                                                                                                                                                                                                                                                                                                                                                         |
| `otel.span.link.count.limit`                                     | `OTEL_SPAN_LINK_COUNT_LIMIT`                                     | `1000`        | Stable  | Maximum number of links per span.                                                                                                                                                                                                                                                                                                                                                                          |

## Sampling configuration

| System property          | Environment variable     | Default value  | Support | Description |
| ------------------------ | ------------------------ | -------------- | ------- | ----------- |
| `otel.traces.sampler`    | `OTEL_TRACES_SAMPLER`     | `always_on`    | Stable  | The sampler to use for tracing.	|

Splunk Distribution of OpenTelemetry Java supports all standard samplers as provided by
[OpenTelemetry Java SDK Autoconfigure](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure#sampler).
In addition, the distribution adds the following samplers:

### `internal_root_off`
Setting `otel.traces.sampler` to `internal_root_off` drops all traces with root spans where `spanKind` is `INTERNAL`, `CLIENT` or `PRODUCER`. This setting only keeps root spans where `spanKind` is `SERVER` and `CONSUMER`.

### `rules`
This sampler allows to ignore individual endpoints and drop all traces that originate from them.
It applies only to spans with `SERVER` kind.

For example, the following configuration results in all requests to `/healthcheck` to be excluded from monitoring:

```shell
export OTEL_TRACES_SAMPLER=rules
export OTEL_TRACES_SAMPLER_ARG=drop=/healthcheck;fallback=parentbased_always_on
```
All requests to downstream services that happen as a consequence of calling an excluded endpoint are also excluded.

The value of `OTEL_TRACES_SAMPLER_ARG` is interpreted as a semicolon-separated list of rules.
The following types of rules are supported:

- `drop=<value>`: The sampler drops a span if its `http.target` attribute has a substring equal to the provided value.
  You can provide as many `drop` rules as you want.
- `fallback=sampler`: Fallback sampler used if no `drop` rule matched a  given span.
  Supported fallback samplers are `always_on` and `parentbased_always_on`.
  Probability samplers such as `traceidratio` are not supported.

> If several `fallback` rules are provided, only the last one will be in effect.

If `OTEL_TRACES_SAMPLER_ARG` is not provided or has en empty value, no `drop` rules are configured and `parentbased_always_on` sampler is as default.

## Java agent configuration

| System property          | Environment variable     | Default value | Support | Description                                      |
|--------------------------|--------------------------|---------------|---------|--------------------------------------------------|
| `otel.javaagent.enabled` | `OTEL_JAVAAGENT_ENABLED` | `true`        | Stable  | Globally enables javaagent auto-instrumentation. |

## Profiler settings

| Setting                                   | Default                       | Description                                                                                                               |
|-------------------------------------------|-------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| `splunk.profiler.enabled`                 | false                         | set to `true` to enable the profiler                                                                                      |
| `splunk.profiler.directory`               | system temp directory         | location of JFR files, defaults to `System.getProperty("java.io.tmpdir")`                                                 |
| `splunk.profiler.recording.duration`      | 20s                           | recording unit duration                                                                                                   |
| `splunk.profiler.keep-files`              | false                         | leave JFR files on disk if `true`                                                                                         |
| `splunk.profiler.logs-endpoint`           | http://localhost:4318/v1/logs | where to send OTLP logs, defaults to `otel.exporter.otlp.endpoint`                                                        |
| `splunk.profiler.call.stack.interval`     | 10000ms                       | how often to sample call stacks                                                                                           |
| `splunk.profiler.memory.enabled`          | false                         | set to `true` to enable all other memory profiling options unless explicitly disabled. Setting to `true` enables metrics. |
| `splunk.profiler.memory.event.rate`       | 150/s                         | allocation event rate                                                                                                     |
| `splunk.profiler.include.internal.stacks` | false                         | set to `true` to include stack traces of agent internal threads and stack traces with only JDK internal frames            |
| `splunk.profiler.tracing.stacks.only`     | false                         | set to `true` to include only stack traces that are linked to a span context                                              |
| `splunk.profiler.otlp.protocol`           | `http/protobuf`               | The transport protocol to use on profiling OTLP log requests. Options include `grpc` and `http/protobuf`.                 |

If the `splunk.profiler.enabled` option is not enabled, all profiling features are disabled. For
example, setting `splunk.profiler.memory.enabled` to `true` has no effect if
`splunk.profiler.enabled` is set to `false`. Similarly, there is no separate toggle for periodic
collection of call stacks (thread dumps), as this feature is enabled only when the profiler is
enabled.

Note: Setting `splunk.profiler.memory.enabled` to `true` automatically activates the splunk metrics.

## Other OpenTelemetry Java agent configuration

You can find all other Java agent configuration options
described [here](https://help.splunk.com/en/splunk-observability-cloud/manage-data/instrument-back-end-services/instrument-back-end-applications-to-send-spans-to-splunk-apm./instrument-a-java-application/configure-the-java-agent).
