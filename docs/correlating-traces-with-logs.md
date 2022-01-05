> The official Splunk documentation for this page is [Connect Java application trace data with logs](https://docs.splunk.com/Observability/gdi/get-data-in/application/java/instrumentation/connect-traces-logs.html#nav-Connect-trace-data-with-logs). For instructions on how to contribute to the docs, see [CONTRIBUTING.md](../CONTRIBUTING.md#documentation).

# Correlating traces with logs

The Java Agent injects several pieces of metadata into the logger MDC.
You can use the following information to correlate traces with logs:

- Trace information: `trace_id` and `span_id`;
- Resource attributes: `service.name` and `deployment.environment`.

Injecting trace context is described in detail on
[this page](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/logger-mdc-instrumentation.md).

To log resource context, the Splunk distribution exposes resource attributes as
system properties prefixed with `otel.resource.` which can be used in logger
configuration.

Example configuration for log4j pattern:

```xml
<PatternLayout>
  <pattern>service: ${sys:otel.resource.service.name}, env: ${sys:otel.resource.deployment.environment} %m%n</pattern>
</PatternLayout>
```

or logback pattern:

```xml
<pattern>service: %property{otel.resource.service.name}, env: %property{otel.resource.deployment.environment}: %m%n</pattern>
```
