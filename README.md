# Splunk distribution of OpenTelemetry Java instrumentation

The Splunk distribution of OpenTelemetry Java Instrumentation provides a
[Java Virtual Machine (JVM) agent](https://docs.oracle.com/javase/7/docs/api/java/lang/instrument/package-summary.html)
that automatically instruments your Java application to capture and
report distributed traces to SignalFx.

## Getting Started

Download the JAR for the agent's [latest
version](https://github.com/signalfx/signalfx-otel-java/releases/latest/download/signalfx-otel-javaagent-all.jar).
and add its path to your JVM startup options:

```bash
$ curl -L https://github.com/signalfx/signalfx-otel-java/releases/latest/download/signalfx-otel-javaagent-all.jar -o signalfx-otel-javaagent.jar
$ java -javaagent:./signalfx-otel-javaagent.jar
```

For more information, see [Configure the SignalFx Java Agent](#Configure-the-SignalFx-Java-Agent).

The agent instruments supported libraries and frameworks with bytecode
manipulation and configures an OpenTelemetry-compatible tracer to capture
and export trace spans. The agent also registers an OpenTelemetry `getTracer`
so you can support existing custom instrumentation or add custom
instrumentation to your application later.

This Splunk distribution comes with the following defaults:

- [B3 context propagation](https://github.com/openzipkin/b3-propagation).
- [Zipkin exporter](https://zipkin.io/zipkin-api/#/default/post_spans)
  configured to send spans to a locally running [SignalFx Smart
  Agent](https://docs.signalfx.com/en/latest/apm/apm-getting-started/apm-smart-agent.html)
  (`http://localhost:9080/api/v2/spans`)

To see the Java Agent in action with sample applications, see our
[examples](https://github.com/signalfx/tracing-examples/tree/master/signalfx-tracing/signalfx-otel-java).

## Requirements and supported software

Specify the agent as the only JVM agent for your application.
If you specify multiple agents, you may encounter issues with at least one
of them.

The agent works with Java runtimes version 8 and higher. Other JVM-based
languages like Scala and Kotlin are also supported, but may not work with all
instrumentations.

Supported libraries and versions are listed
[here](https://github.com/open-telemetry/opentelemetry-java-instrumentation#supported-java-libraries-and-frameworks).

## Configuring the Java Agent

Send traces from your Java application to the SignalFx Smart Agent or
OpenTelemetry Collector.

### Configuration values

The agent needs the following properties or environment variables for configuring
tracer functionality and trace content. System property values take priority
over corresponding environment variables.

| System property | Environment variable | Default value | Notes |
| ---             | ---                  | ---           | ---   |
| `signalfx.tracing.enabled` | `SIGNALFX_TRACING_ENABLED` | `"true"` | Globally enables tracer creation and auto-instrumentation.  Any value not matching `"true"` is treated as false (`Boolean.valueOf()`). |
| `signalfx.span.tags` | `SIGNALFX_SPAN_TAGS` | `null` | Comma-separated list of tags included in every reported span. For example, `"key1:val1,key2:val2"`. |
| `signalfx.db.statement.max.length` | `SIGNALFX_DB_STATEMENT_MAX_LENGTH` | `1024` | The maximum number of characters written for the OpenTracing `db.statement` tag. |
| `signalfx.recorded.value.max.length` | `SIGNALFX_RECORDED_VALUE_MAX_LENGTH` | `12288` | The maximum number of characters for any Zipkin-encoded tagged or logged value. |
| `signalfx.trace.annotated.method.blacklist` | `SIGNALFX_TRACE_ANNOTATED_METHOD_BLACKLIST` | `null` | Prevents `@Trace` annotation functionality for the target method string of format `package.OuterClass[methodOne,methodTwo];other.package.OuterClass$InnerClass[*];`. (; is required and * for all methods in class). |
| `signalfx.trace.methods` | `SIGNALFX_TRACE_METHODS` | `null` | Same as adding `@Trace` annotation functionality for the target method string of format `package.OuterClass[methodOne,methodTwo];other.package.OuterClass$InnerClass[*];`. (; is required and * for all public methods in class). |
| `signalfx.max.spans.per.trace` | `SIGNALFX_MAX_SPANS_PER_TRACE` | `0 (no limit)` | Drops traces with more spans than this. Intended to prevent runaway traces from flooding upstream systems. |
| `signalfx.max.continuation.depth` | `SIGNALFX_MAX_CONTINUATION_DEPTH` | `100` | Stops propagating asynchronous context at this recursive depth. Intended to prevent runaway traces from leaking memory. |

#### Zipkin exporter

The Zipkin exporter [POSTs
JSON](https://zipkin.io/zipkin-api/#/default/post_spans) to a specified HTTP
URL.

| System property            | Environment variable       | Default value                        | Notes                                                                |
| -------------------------- | -------------------------- | ------------------------------------ | -------------------------------------------------------------------- |
| otel.zipkin.endpoint       | OTEL_ZIPKIN_ENDPOINT       | `http://localhost:9080/api/v2/spans` | The Zipkin endpoint to connect to. Currently only HTTP is supported. |
| otel.zipkin.service.name   | OTEL_ZIPKIN_SERVICE_NAME   | `unknown`                            | The service name of this JVM instance.                               |

#### Peer service name

The [peer service
name](https://github.com/open-telemetry/opentelemetry-specification/blob/master/specification/trace/semantic_conventions/span-general.md#general-remote-service-attributes)
is the name of a remote service that is being connected to. It corresponds to
`service.name` in the
[Resource](https://github.com/open-telemetry/opentelemetry-specification/tree/master/specification/resource/semantic_conventions#service)
for the local service.

| System property                     | Environment variable               | Purpose                                                                      |
|-------------------------------------|------------------------------------|------------------------------------------------------------------------------|
| otel.endpoint.peer.service.mapping  | OTEL_ENDPOINT_PEER_SERVICE_MAPPING | Used to specify a mapping from hostnames or IP addresses to peer services, as a comma separated list of host=name pairs. The peer service name will be added as an attribute to a span whose host or IP match the mapping. For example, if set to 1.2.3.4=cats-service,dogs-abcdef123.serverlessapis.com=dogs-api, requests to `1.2.3.4` will have a `peer.service` attribute of `cats-service` and requests to `dogs-abcdef123.serverlessapis.com` will have one of `dogs-api` |

#### Trace config

| System property                   | Environment variable              | Default value  | Purpose                                |
| --------------------------------- | --------------------------------- | -------------- | -------------------------------------- |
| otel.config.max.attrs             | OTEL_CONFIG_MAX_ATTRS             | 32             | Maximum number of attributes per span  |
| otel.config.max.events            | OTEL_CONFIG_MAX_EVENTS            | 128            | Maximum number of events per span      |
| otel.config.max.links             | OTEL_CONFIG_MAX_LINKS             | 32             | Maximum number of links per span       |
| otel.config.max.event.attrs       | OTEL_CONFIG_MAX_EVENT_ATTRS       | 32             | Maximum number of attributes per event |
| otel.config.max.link.attrs        | OTEL_CONFIG_MAX_LINK_ATTRS        | 32             | Maximum number of attributes per link  |

## Manually instrument a Java application

Documentation on how to manually instrument a Java application are available
[here](https://github.com/open-telemetry/opentelemetry-java-instrumentation#manually-instrumenting).

## Troubleshooting the Java Agent

To turn on the agent's internal debug logging:

`-Dio.opentelemetry.javaagent.slf4j.simpleLogger.defaultLogLevel=debug`

Note these logs are extremely verbose. Enable debug logging only when needed.
Debug logging negatively impacts the performance of your application.

# License and versioning

The Splunk distribution of OpenTelemetry Java Instrumentation is a distribution
of the [OpenTelemetry Java Instrumentation
project](https://github.com/open-telemetry/opentelemetry-java-instrumentation).
It is released under the terms of the Apache Software License version 2.0. See
[the license file](./LICENSE) for more details.
