# Migrate from the SignalFx Java Agent

The Splunk Distribution of OpenTelemetry Java Instrumentation replaces the
[SignalFx Java Agent](https://github.com/signalfx/signalfx-java-tracing).
If you're using the SignalFx Java Agent, migrate to the Splunk Distribution of
OpenTelemetry Java Instrumentation to use OpenTelemetry's instrumentation to
send traces to Splunk APM. The Splunk Distribution of OpenTelemetry Java
Instrumentation uses OpenTelemetry to instrument applications, which is an
open-source API to gather telemetry data, and has a smaller footprint.

Because the SignalFx Java Agent uses OpenTracing and the Splunk Distribution
of OpenTelemetry Java Instrumentation uses OpenTelemetry, the semantic
conventions for span tag names change when you migrate. For more information,
see [Migrate from OpenTracing to OpenTelemetry](https://docs.signalfx.com/en/latest/apm/apm-getting-started/apm-opentelemetry-collector.html#apm-opentelemetry-migration).

The SignalFx Java Agent and the Splunk Distribution of OpenTelemetry Java
Instrumentation could export trace data in different formats. This would
result in your application sending different or incomplete data to SignalFx.
To address this, you could run a pilot test to compare the trace data you
receive with the Splunk Distribution of OpenTelemetry Java Instrumentation
against the data you used to receive from the SignalFx Java Agent.

## Steps to migrate

Follow these steps to migrate from the SignalFx Java Agent to the Splunk
distribution of Splunk Distribution of OpenTelemetry Java Instrumentation:

1. Download the [latest release](https://github.com/signalfx/splunk-otel-java/releases)
   of the Splunk Distribution of OpenTelemetry Java Instrumentation.
2. Set the service name. This is how you can identify the service in APM.
   You can set the service name with a system property or environment
   variable. This is how you can set it with an environment variable with a
   service name of `yourServiceName`:
   ```
   $ EXPORT OTEL_EXPORTER_ZIPKIN_SERVICE_NAME="yourServiceName"
   ```
3. Specify the endpoint of the SignalFx Smart Agent or OpenTelemetry Collector
   you're exporting traces to. You can set the endpoint with a system property
   or environment variable. This is how you can set it with an environment
   variable with an endpoint of `http://yourEndpoint:9080/v1/trace`:
   ```
   $ EXPORT OTEL_EXPORTER_ZIPKIN_ENDPOINT="http://yourEndpoint:9080/v1/trace"
   ```
   The default value is `http://localhost:9080/v1/trace`. If you're exporting
   traces to a local Smart Agent, you don't have to modify this configuration
   setting.
4. In your application startup script, replace `-javaagent:./signalfx-tracing.jar`
   with `-javaagent:/path/to/splunk-otel-javaagent-all.jar`.

## Changes in functionality

Each of the following sections describe any changes in functionality as you
migrate from the SignalFx Java Agent to the Splunk Distribution of
OpenTelemetry Java Instrumentation.

### Configuration setting changes

These SignalFx Java Agent system properties correspond to the following
OpenTelemetry system properties:

| SignalFx system property | OpenTelemetry system property |
| ------------------------ | ----------------------------- |
| `signalfx.service.name` | `otel.exporter.zipkin.service.name` |
| `signalfx.endpoint.url` | `otel.exporter.zipkin.endpoint` |
| `signalfx.tracing.enabled` | `otel.trace.enabled` |
| `signalfx.span.tags` | `otel.resource.attributes` |
| `signalfx.recorded.value.max.length` | `otel.config.max.attr.length` |
| `signalfx.db.statement.max.length` | `otel.config.max.attr.length` | 
| `signalfx.trace.annotated.method.blacklist` | `otel.trace.annotated.methods.exclude` |
| `signalfx.trace.methods` | `otel.trace.methods` |
| `signalfx.integration.<name>.enabled=true` | `otel.integration.[id].enabled=false` | 

These SignalFx Java Agent environment variables correspond to the following
OpenTelemetry environment variables:

| SignalFx environment variable | OpenTelemetry environment variable |
| ----------------------------- | ---------------------------------- |
| `SIGNALFX_SERVICE_NAME` | `OTEL_EXPORTER_ZIPKIN_SERVICE_NAME` |
| `SIGNALFX_ENDPOINT_URL` |`OTEL_EXPORTER_ZIPKIN_ENDPOINT` |
| `SIGNALFX_TRACING_ENABLED` | `OTEL_TRACE_ENABLED` |
| `SIGNALFX_SPAN_TAGS` | `OTEL_RESOURCE_ATTRIBUTES` |
| `SIGNALFX_RECORDED_VALUE_MAX_LENGTH` | `OTEL_CONFIG_MAX_ATTR_LENGTH` |
| `SIGNALFX_DB_STATEMENT_MAX_LENGTH` | `OTEL_CONFIG_MAX_ATTR_LENGTH` |
| `SIGNALFX_TRACE_ANNOTATED_METHOD_BLACKLIST` | `OTEL_TRACE_ANNOTATED_METHODS_EXCLUDE` |
| `SIGNALFX_TRACE_METHODS` | `OTEL_TRACE_METHODS` |

These SignalFx Java Agent system properties and environment variables don't
have corresponding configuration options with the Spunk Distribution for
OpenTelemetry Java Instrumentation:

| System property | Environment variable |
| --------------- | -------------------- |
| `signalfx.agent.host` | `SIGNALFX_AGENT_HOST` |
| `signalfx.db.statement.max.length` | `SIGNALFX_DB_STATEMENT_MAX_LENGTH` |
| `signalfx.max.continuation.depth` | `SIGNALFX_MAX_CONTINUATION_DEPTH` |

### Log injection changes

You can inject trace IDs in logs with the Splunk Distribution of OpenTelemetry
Java Instrumentation, but the list of compatible logging frameworks is
different:

| Old logging framework |
| --------------------- |
| `logback` |
| `log4j` |
| `slf4j` |

| New logging framework | Version |
| --------------------- | ------- |
| `log4j 1` | 1.2+ |
| `log4j 2` | 2.7+ |
| `logback` | 1.0+ |

For more information about injecting trace IDs in logs with the Splunk
Distribution of OpenTelemetry Java Instrumentation, see
[Logger MDC auto-instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/master/docs/logger-mdc-instrumentation.md).

### Trace annotation changes

The `@Trace` annotation that the SignalFx Java Agent uses is compatible with
the Splunk Distribution of OpenTelemetry Java Instrumentation. If you're using
the `@Trace` annotation for custom instrumentation, you don't have to make any
changes to maintain existing functionality. 

If you want to configure new custom instrumentation and don't want to use the
OpenTelemetry `getTracer` and API directly, use the OpenTelemetry `@WithSpan`
annotation instead of the `@Trace` annotation. For more information, see
[Configure a WithSpan annotation](https://github.com/open-telemetry/opentelemetry-java-instrumentation#configure-a-withspan-annotation).

The `@TraceSetting` annotation to allow an exception isn't supported.
