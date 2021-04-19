# Migrate from the SignalFx Java Agent

The Splunk Distribution of OpenTelemetry Java Instrumentation replaces the
[SignalFx Java Agent](https://github.com/signalfx/signalfx-java-tracing).
If you're using the SignalFx Java Agent, migrate to the Splunk Distribution of
OpenTelemetry Java Instrumentation.

The distribution is based on the [OpenTelemetry Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation):
an open-source project that uses the OpenTelemetry API and has a smaller memory
footprint than the SignalFx Java Agent.

## Telemetry data

Because the SignalFx Java Agent uses OpenTracing and the Splunk Distribution
of OpenTelemetry Java Instrumentation uses OpenTelemetry, the semantic
conventions for span tag names change when you migrate. For more information,
see [Migrate from OpenTracing to OpenTelemetry](https://docs.signalfx.com/en/latest/apm/apm-getting-started/apm-opentelemetry-collector.html#apm-opentelemetry-migration).

Depending on the configuration, the SignalFx Java Agent and the Splunk Distribution of OpenTelemetry Java
Instrumentation may export trace data in different formats. Such a situation would
result in your application sending different data to Splunk APM.
To address this, please run a pilot test to compare the trace data you
receive with the Splunk Distribution of OpenTelemetry Java Instrumentation
against the data you used to receive from the SignalFx Java Agent.

## Steps to migrate

Follow these steps to migrate from the SignalFx Java Agent to the Splunk
distribution of Splunk Distribution of OpenTelemetry Java Instrumentation:

1. Download the [latest release](https://github.com/signalfx/splunk-otel-java/releases/latest/download/splunk-otel-javaagent-all.jar)
   of the Splunk Distribution of OpenTelemetry Java Instrumentation. For example use: 
   ```bash
   $ # download the newest version of the agent
   $ curl -vsSL -o splunk-otel-javaagent-all.jar 'https://github.com/signalfx/splunk-otel-java/releases/latest/download/splunk-otel-javaagent-all.jar'
   ```
2. Set the service name. This is how you can identify the service in Splunk APM.

   An example how to set it using an environment variable:
   ```bash
   $ EXPORT OTEL_RESOURCE_ATTRIBUTES=service.name=my-java-app
   ```
   or a system property:
   ```
   -Dotel.resource.attributes=service.name=my-java-app
   ```
3. Specify the endpoint of the SignalFx Smart Agent or OpenTelemetry Collector
   you're exporting traces to. You can set the endpoint with a system property
   or environment variable. 
   
   An example how to set it using an environment variable:
   ```
   $ EXPORT OTEL_EXPORTER_JAEGER_ENDPOINT="http://yourEndpoint:9080/v1/trace"
   ```
   or a system property:
   ```
   -Dotel.exporter.jaeger.endpoint=http://yourEndpoint:9080/v1/trace
   ```
   The default value is `http://localhost:9080/v1/trace`. If you're exporting
   traces to a local Smart Agent, you don't have to modify this configuration
   setting.
4. In your application startup script, replace `-javaagent:./signalfx-tracing.jar`
   with `-javaagent:/path/to/splunk-otel-javaagent-all.jar`.
5. If you manually instrumented any code with an OpenTracing tracer, expose
   the OpenTelemetry tracer as an implementation of an OpenTracing tracer with
   the OpenTracing Shim. For more information, see
   [OpenTelemetry - OpenTracing Shim](https://github.com/open-telemetry/opentelemetry-java/tree/main/opentracing-shim).
   If you use another API for manual instrumentation, such as for the `@Trace`
   annotation in the SignalFx Java Agent, ensure it's in your application's
   `classpath` as well. For an example of what this looks like, see this
   [SignalFx Java Agent example application](https://github.com/signalfx/tracing-examples/blob/main/signalfx-tracing/signalfx-java-tracing/okhttp-and-jedis/src/main/java/com/signalfx/tracing/examples/javaagent/App.java).

## Changes in functionality

Each of the following sections describe any changes in functionality as you
migrate from the SignalFx Java Agent to the Splunk Distribution of
OpenTelemetry Java Instrumentation.

### Configuration setting changes

These SignalFx Java Agent system properties correspond to the following
OpenTelemetry system properties (NOTE: some properites are exporter-specific, the default is `jaeger`):

| SignalFx system property | OpenTelemetry system property |
| ------------------------ | ----------------------------- |
| `signalfx.service.name` | `otel.resource.attributes=service.name=<name of the service>` |
| `signalfx.env` | `otel.resource.attributes=deployment.environment=<name of the environment>` |
| `signalfx.endpoint.url` | `otel.exporter.jaeger.endpoint` |
| `signalfx.tracing.enabled` | `otel.javaagent.enabled` |
| `signalfx.integration.<name>.enabled=false` | `otel.instrumentation.<id>.enabled=false` | 
| `signalfx.span.tags` | `otel.resource.attributes=<comma separated key=value pairs>` |
| `signalfx.trace.annotated.method.blacklist` | `otel.trace.annotated.methods.exclude` |
| `signalfx.trace.methods` | `otel.trace.methods` |
| `signalfx.server.timing.context` | `splunk.context.server-timing.enabled` |

Note: when setting both `service name` and `environment` appropriate `otel.resource.attributes` property setting will 
look like this: `otel.resource.attributes=service.name=myService,deployment.environment=myEnvironment` 

Additional info about disabling a particular instrumentation can be found in the [OpenTelemetry Java Instrumentation docs](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/suppressing-instrumentation.md).

These SignalFx Java Agent environment variables correspond to the following
OpenTelemetry environment variables:

| SignalFx environment variable | OpenTelemetry environment variable |
| ----------------------------- | ---------------------------------- |
| `SIGNALFX_SERVICE_NAME` | `OTEL_RESOURCE_ATTRIBUTES=service.name=<name of the service>` |
| `SIGNALFX_ENV` | `OTEL_RESOURCE_ATTRIBUTES=deployment.environment=<name of the environment>` |
| `SIGNALFX_ENDPOINT_URL` |`OTEL_EXPORTER_JAEGER_ENDPOINT` |
| `SIGNALFX_TRACING_ENABLED` | `OTEL_JAVAAGENT_ENABLED` |
| `SIGNALFX_INTEGRATION_<name>_ENABLED=false` | `OTEL_INSTRUMENTATION_<id>_ENABLED=false` |
| `SIGNALFX_SPAN_TAGS` | `OTEL_RESOURCE_ATTRIBUTES` |
| `SIGNALFX_TRACE_ANNOTATED_METHOD_BLACKLIST` | `OTEL_TRACE_ANNOTATED_METHODS_EXCLUDE` |
| `SIGNALFX_TRACE_METHODS` | `OTEL_TRACE_METHODS` |
| `SIGNALFX_SERVER_TIMING_CONTEXT` | `SPLUNK_CONTEXT_SERVER_TIMING_ENABLED` |

These SignalFx Java Agent system properties and environment variables don't
have corresponding configuration options with the Spunk Distribution for
OpenTelemetry Java Instrumentation:

| System property | Environment variable |
| --------------- | -------------------- |
| `signalfx.agent.host` | `SIGNALFX_AGENT_HOST` |
| `signalfx.db.statement.max.length` | `SIGNALFX_DB_STATEMENT_MAX_LENGTH` |
| `signalfx.recorded.value.max.length` | `SIGNALFX_RECORDED_VALUE_MAX_LENGTH` |
| `signalfx.max.spans.per.trace` | `SIGNALFX_MAX_SPANS_PER_TRACE` |
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
