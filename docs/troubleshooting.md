> The official Splunk documentation for this page is [Troubleshooting Java instrumentation](https://docs.splunk.com/Observability/gdi/get-data-in/application/java/troubleshooting/common-java-troubleshooting.html). For instructions on how to contribute to the docs, see [CONTRIBUTE.md](../CONTRIBUTE.md).

# Troubleshooting

Before you create an issue or open a support request, please try to gather as much information about your situation as
possible. Try to include the following information:

* What did you try to do?
* What happened?
* What did you expect to happen?
* Have you found any workaround?
* How impactful is the issue?
* How can we reproduce the issue?
* What OS, JVM, libraries, frameworks, application servers are you using?
* What [configuration options](advanced-config.md) were used?

Please include the full [javaagent debug logs](#javaagent-debug) if possible.

## Javaagent debug

To turn on the agent's internal debug logging:

`-Dotel.javaagent.debug=true`

> :warning: Debug logging is extremely verbose and resource intensive. Enable
> debug logging only when needed and disable when done.

## Instrumentation issues

If you find something wrong with a particular instrumentation (or suspect that there's something wrong with it) you can
suppress it by following steps
described [here](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/suppressing-instrumentation.md#suppressing-specific-agent-instrumentation)
.

If you find that any instrumentation is broken please do not hesitate
to [file a bug](https://github.com/signalfx/splunk-otel-java/issues/new).

## Trace exporter issues

If you're unsure which trace exporter you are using, most likely it's the OTLP exporter - it's the default trace
exporter in the Splunk Distribution of OpenTelemetry Java.

### OTLP exporter

If you're seeing the following error in your logs:

```
[BatchSpanProcessor_WorkerThread-1] ERROR io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter - Failed to export spans. Server is UNAVAILABLE. Make sure your collector is running and reachable from this network. Full error message:UNAVAILABLE: io exception
```

then it means that the javaagent cannot send trace data to the OpenTelemetry Collector.

1. Please make sure that `otel.exporter.otlp.endpoint` points to the correct OpenTelemetry Collector instance host.
2. Please verify that your Collector instance is up.
3. Please make sure that your OpenTelemetry Collector instance is properly configured and that the OTLP gRPC receiver is
   enabled and plugged into the traces pipeline.
4. The OpenTelemetry Collector listens on the following address: `http://<host>:4317`. Verify that your URL is correct.

If you're seeing the following error in your logs:

```
[grpc-default-executor-1] ERROR io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter - Failed to export spans. Server is UNAVAILABLE. Make sure your collector is running and reachable from this network. Full error message:UNAVAILABLE: io exception
Channel Pipeline: [SslHandler#0, ProtocolNegotiators$ClientTlsHandler#0, WriteBufferingAndExceptionHandler#0, DefaultChannelPipeline$TailContext#0]
```

then you're probably trying to send traces directly to the Splunk ingest; unfortunately, the OTLP span exporter does not
support this scenario yet. Please use the [Jaeger exporter](advanced-config.md#trace-exporters) instead.

### Jaeger exporter

If you're seeing the following warnings in your logs:

```
[BatchSpanProcessor_WorkerThread-1] WARN io.opentelemetry.exporter.jaeger.thrift.JaegerThriftSpanExporter - Failed to export spans
io.jaegertracing.internal.exceptions.SenderException: Could not send 8 spans
	at io.jaegertracing.thrift.internal.senders.HttpSender.send(HttpSender.java:69)
	...
Caused by: java.net.ConnectException: Failed to connect to localhost/0:0:0:0:0:0:0:1:9080
	at okhttp3.internal.connection.RealConnection.connectSocket(RealConnection.java:265)
	...
Caused by: java.net.ConnectException: Connection refused (Connection refused)
	...
```

then it means that the javaagent cannot send trace data to the Smart Agent/OpenTelemetry Collector/Splunk cloud.

1. Please make sure that `otel.exporter.jaeger.endpoint` points to the correct host:
   a Smart Agent or OpenTelemetry Collector instance, or the Splunk ingest URL.
2. If you're using the Agent or Collector, verify that the instance is up.
3. Please make sure that your Agent/Collector instance is properly configured and the Jaeger Thrift HTTP receiver is
   enabled and plugged into the traces pipeline.
4. Smart Agent and OpenTelemetry Collector by default use different ports (and paths)
   for the Jaeger receiver: the Agent uses `http://<host>:9080/v1/trace` and the Collector
   uses `http://<host>:14268/api/traces`. Verify that your URL is correct.

If you're sending spans directly to the Splunk cloud and getting the following errors:

```
[BatchSpanProcessor_WorkerThread-1] WARN io.opentelemetry.exporter.jaeger.thrift.JaegerThriftSpanExporter - Failed to export spans
io.jaegertracing.internal.exceptions.SenderException: Could not send 40 spans, response 401: Unauthorized
	at io.jaegertracing.thrift.internal.senders.HttpSender.send(HttpSender.java:86)
	...
```

then it means that your `SPLUNK_ACCESS_TOKEN` setting is either missing or invalid. Please make sure that you use a
valid Splunk access token when sending telemetry directly to the Splunk cloud.

## Metrics exporter issues

If you see the following warning:

```
[signalfx-metrics-publisher] WARN com.splunk.javaagent.shaded.io.micrometer.signalfx.SignalFxMeterRegistry - failed to send metrics: Unable to send datapoints
```

in your logs it means that the javaagent cannot send metrics to your Smart Agent, OpenTelemetry Collector or the Splunk
backend.

1. Please make sure that `splunk.metrics.endpoint` points to the correct host:
   a Smart Agent or OpenTelemetry Collector instance, or the Splunk ingest URL.
2. If you're using the Agent or Collector, verify that the instance is up.
3. Please make sure that your Agent/Collector instance is properly configured and the SignalFx receiver is enabled and
   plugged into the metrics pipeline.
4. Smart Agent and OpenTelemetry Collector by default use different ports for the SignalFx receiver: the Agent
   uses `http://<host>:9080/v2/datapoint`
   and the Collector uses `http://<host>:9943`. Verify that your URL is correct.
5. If you're sending metrics directly to the Splunk cloud, please verify that the `SPLUNK_ACCESS_TOKEN` is configured
   and contains a valid access token.
6. Metrics feature is still experimental - if you can't make it work or encounter any unexpected issues you
   can [turn it off](advanced-config.md#splunk-distribution-configuration)
   and [file a bug](https://github.com/signalfx/splunk-otel-java/issues/new).
