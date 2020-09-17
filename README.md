# Splunk distribution of OpenTelemetry Java Instrumentation

The Splunk distribution of [OpenTelemetry Java
Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
provides a [Java Virtual Machine (JVM)
agent](https://docs.oracle.com/javase/7/docs/api/java/lang/instrument/package-summary.html)
that automatically instruments your Java application to capture and report
distributed traces to SignalFx APM.

This Splunk distribution comes with the following defaults:

- [B3 context propagation](https://github.com/openzipkin/b3-propagation).
- [Zipkin exporter](https://zipkin.io/zipkin-api/#/default/post_spans)
  configured to send spans to a locally running [SignalFx Smart
  Agent](https://docs.signalfx.com/en/latest/apm/apm-getting-started/apm-smart-agent.html)
  (`http://localhost:9080/v1/trace`).

> :warning: This project is currently in **BETA**

## Getting Started

The agent works with Java runtimes version 8 and higher. Supported libraries
and versions are listed
[here](https://github.com/open-telemetry/opentelemetry-java-instrumentation#supported-java-libraries-and-frameworks).
Other JVM-based languages like Scala and Kotlin are also supported, but may not
work with all instrumentations.

> :warning: Specify the agent as the only JVM agent for your application.
> Specifying multiple agents may result in unexpected behavior.

To get started, download the JAR for the agent's [latest
version](https://github.com/signalfx/splunk-otel-java/releases/latest/download/splunk-otel-javaagent-all.jar)
and add its path to your JVM startup options.

For example, if the runtime parameters were:

```bash
$ java -jar target/java-agent-example-1.0-SNAPSHOT-shaded.jar https://google.com
```

Then the updates would be:

```bash
$ curl -L https://github.com/signalfx/splunk-otel-java/releases/latest/download/splunk-otel-javaagent-all.jar \
    -o splunk-otel-javaagent.jar
$ java -javaagent:./splunk-otel-javaagent.jar -Dotel.zipkin.service.name=my-java-app \
    -jar target/java-agent-example-1.0-SNAPSHOT-shaded.jar https://google.com
```

The agent instruments supported libraries and frameworks with bytecode
manipulation and configures an OpenTelemetry-compatible tracer to capture
and export trace spans. The agent also registers an OpenTelemetry `getTracer`
so you can support existing custom instrumentation or add custom
instrumentation to your application later.

To see the Java Agent in action with sample applications, see our
[examples](https://github.com/signalfx/tracing-examples/tree/master/signalfx-tracing/splunk-otel-java).

## All configuration options

The agent offers the following properties or environment variables. System property values take priority
over corresponding environment variables. Only the service name needs to be updated.

### Zipkin exporter

The Zipkin exporter [POSTs
JSON](https://zipkin.io/zipkin-api/#/default/post_spans) to a specified HTTP
URL.

| System property            | Environment variable       | Default value                        | Notes                                                                |
| -------------------------- | -------------------------- | ------------------------------------ | -------------------------------------------------------------------- |
| otel.zipkin.endpoint       | OTEL_ZIPKIN_ENDPOINT       | `http://localhost:9080/v1/trace`     | The Zipkin endpoint to connect to. Currently only HTTP is supported. |
| otel.zipkin.service.name   | OTEL_ZIPKIN_SERVICE_NAME   | `unknown`                            | The service name of this JVM instance.                               |

### Trace configuration

| System property                     | Environment variable               | Default value  | Purpose                                                                                                                                                                                                                                                                                                                                                                                                   |
| ----------------------------------- | ---------------------------------- | -------------- | ------------------------------------------------------------------------------------                                                                                                                                                                                                                                                                                                                      |
| otel.config.max.attrs               | OTEL_CONFIG_MAX_ATTRS              | unlimited      | Maximum number of attributes per span.                                                                                                                                                                                                                                                                                                                                                                    |
| otel.config.max.attr.length         | OTEL_CONFIG_MAX_ATTR_LENGTH        | unlimited      | Maximum length of string attribute value in characters. Longer values are truncated.                                                                                                                                                                                                                                                                                                                      |
| otel.config.max.events              | OTEL_CONFIG_MAX_EVENTS             | `256`          | Maximum number of events per span.                                                                                                                                                                                                                                                                                                                                                                        |
| otel.config.max.links               | OTEL_CONFIG_MAX_LINKS              | `256`          | Maximum number of links per span.                                                                                                                                                                                                                                                                                                                                                                         |
| otel.config.max.event.attrs         | OTEL_CONFIG_MAX_EVENT_ATTRS        | unlimited      | Maximum number of attributes per event.                                                                                                                                                                                                                                                                                                                                                                   |
| otel.config.max.link.attrs          | OTEL_CONFIG_MAX_LINK_ATTRS         | unlimited      | Maximum number of attributes per link.                                                                                                                                                                                                                                                                                                                                                                    |
| otel.endpoint.peer.service.mapping  | OTEL_ENDPOINT_PEER_SERVICE_MAPPING | unset          | Used to add a `peer.service` attribute by specifing a comma separated list of mapping from hostnames or IP addresses. <details><summary>Example</summary>If set to `1.2.3.4=cats-service,dogs-service.serverlessapis.com=dogs-api`, requests to `1.2.3.4` will have a `peer.service` attribute of `cats-service` and requests to `dogs-service.serverlessapis.com` will have one of `dogs-api`.</details> |
| otel.resource.attributes            | OTEL_RESOURCE_ATTRIBUTES           | unset          | Comma-separated list of resource attributes added to every reported span. <details><summary>Example</summary>`key1=val1,key2=val2`</details>
| otel.trace.enabled                  | OTEL_TRACE_ENABLED                 | `true`         | Globally enables tracer creation and auto-instrumentation.                                                                                                                                                                                                                                                                                                                                                |
| otel.trace.methods                  | OTEL_TRACE_METHODS                 | unset          | Same as adding `@WithSpan` annotation functionality for the target method string. <details><summary>Format</summary>`my.package.MyClass1[method1,method2];my.package.MyClass2[method3]`</details>                                                                                                                                                                                                            |
| otel.trace.annotated.methods.exclude     | OTEL_TRACE_ANNOTATED_METHODS_EXCLUDE    | unset          | Suppress `@WithSpan` instrumentation for specific methods. <details><summary>Format</summary>`my.package.MyClass1[method1,method2];my.package.MyClass2[method3]`</details>                                                                                                                                                                                                                                |

## Manually instrument a Java application

Documentation on how to manually instrument a Java application are available
[here](https://github.com/open-telemetry/opentelemetry-java-instrumentation#manually-instrumenting).

## Troubleshooting the Java Agent

To turn on the agent's internal debug logging:

`-Dio.opentelemetry.javaagent.slf4j.simpleLogger.defaultLogLevel=debug`

> :warning: Debug logging is extremely verbose and resource intensive. Enable
> debug logging only when needed and disable when done.

# License and versioning

The Splunk distribution of OpenTelemetry Java Instrumentation is a distribution
of the [OpenTelemetry Java Instrumentation
project](https://github.com/open-telemetry/opentelemetry-java-instrumentation).
It is released under the terms of the Apache Software License version 2.0. See
[the license file](./LICENSE) for more details.
