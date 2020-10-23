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

## Steps

Follow these steps to migrate from the SignalFx Java Agent to the Splunk
distribution of Splunk Distribution of OpenTelemetry Java Instrumentation:

1. Download the [latest release](https://github.com/signalfx/splunk-otel-java/releases)
   of the Splunk Distribution of OpenTelemetry Java Instrumentation.
2. Set the service name. This is how you can identify the service in APM.
   You can set the service name with a system property or environment
   variable. This is how you can set it with an environment variable with a
   service name of `yourServiceName`:
   ```
   $ EXPORT OTEL_ZIPKIN_SERVICE_NAME="yourServiceName"
   ```
3. Specify the endpoint of the SignalFx Smart Agent or OpenTelemetry Collector
   you're exporting traces to. You can set the endpoint with a system property
   or environment variable. This is how you can set it with an environment
   variable with an endpoint of `http://yourEndpoint:9080/v1/trace`:
   ```
   $ EXPORT OTEL_ZIPKIN_ENDPOINT="http://yourEndpoint:9080/v1/trace"
   ```
   The default value is `http://localhost:9080/v1/trace`. If you're exporting
   traces to a local Smart Agent, you don't have to modify this configuration
   setting.
4. In your application startup script, replace `-javaagent:./signalfx-tracing.jar`
   with `-javaagent:/path/to/splunk-otel-javaagent-all.jar`.