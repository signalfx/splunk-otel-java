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

If you find something wrong with a particular instrumentation (or suspect that
there's something wrong with it) you can suppress it by following steps
described [here](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/suppressing-instrumentation.md#suppressing-specific-agent-instrumentation).

If you find that any instrumentation is broken please do not hesitate to [file a bug](https://github.com/signalfx/splunk-otel-java/issues/new).

## Trace exporter issues

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

then it means that the javaagent cannot send trace data to the Smart Agent/Collector/Splunk backend.

Assuming you're using the default Jaeger Thrift exporter:

1. Please make sure that `otel.exporter.jaeger.endpoint` points to the correct host:
   a Smart Agent or OpenTelemetry Collector instance, or the Splunk ingest URL.
2. If you're using the Agent or Collector, verify that the instance is up.
3. Please make sure that your Agent/Collector instance is properly configured and the Jaeger Thrift HTTP receiver is
   enabled and plugged into the traces pipeline.
4. Smart Agent and OpenTelemetry Collector by default use different ports (and paths)
   for the Jaeger receiver: the Agent uses `http://<host>:9080/v1/trace` and the Collector
   uses `http://<host>:14268/api/traces`. Verify that your URL is correct.

## Metrics exporter issues

If you see the following warning:

```
[signalfx-metrics-publisher] WARN com.splunk.javaagent.shaded.io.micrometer.signalfx.SignalFxMeterRegistry - failed to send metrics: Unable to send datapoints
```

in your logs it means that the javaagent cannot send metrics to your Smart Agent, OpenTelemetry Collector
or the Splunk backend.

1. Please make sure that `splunk.metrics.endpoint` points to the correct host:
   a Smart Agent or OpenTelemetry Collector instance, or the Splunk ingest URL.
2. If you're using the Agent or Collector, verify that the instance is up.
3. Please make sure that your Agent/Collector instance is properly configured
   and the SignalFx receiver is enabled and plugged into the metrics pipeline.
4. Smart Agent and OpenTelemetry Collector by default use different ports
   for the SignalFx receiver: the Agent uses `http://<host>:9080/v2/datapoint`
   and the Collector uses `http://<host>:9943`. Verify that your URL is correct.
5. Metrics feature is still experimental - if you can't make it work or encounter
   any unexpected issues you can [turn it off](advanced-config.md#splunk-distribution-configuration)
   and [file a bug](https://github.com/signalfx/splunk-otel-java/issues/new).
