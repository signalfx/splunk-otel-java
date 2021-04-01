# Splunk distribution of OpenTelemetry Java Instrumentation

The Splunk distribution of [OpenTelemetry Java
Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
provides a [Java Virtual Machine (JVM)
agent](https://docs.oracle.com/javase/7/docs/api/java/lang/instrument/package-summary.html)
that automatically instruments your Java application to capture and report
distributed traces to Splunk APM.

If you're currently using the SignalFx Java Agent and want to
migrate to the Splunk Distribution of OpenTelemetry Java Instrumentation,
see [Migrate from the SignalFx Java Agent](MIGRATING.md).

This Splunk distribution comes with the following defaults:

- [B3 context propagation](https://github.com/openzipkin/b3-propagation).
- [Jaeger-Thrift exporter](https://www.jaegertracing.io)
  configured to send spans to a locally running [SignalFx Smart
  Agent](https://docs.signalfx.com/en/latest/apm/apm-getting-started/apm-smart-agent.html)
  (`http://localhost:9080/v1/trace`).
- Unlimited default limits for [configuration options](#trace-configuration) to
  support full-fidelity traces.

> :construction: This project is currently in **BETA**.

## Getting Started

To get started, download the JAR for the agent's [latest
version](https://github.com/signalfx/splunk-otel-java/releases/latest/download/splunk-otel-javaagent-all.jar)
and add its path to your JVM startup options.

For example, if the runtime parameters were:

```bash
$ java -jar target/java-agent-example-1.0-SNAPSHOT-shaded.jar https://google.com
```

Then the runtime parameters would be updated to:

```bash
$ curl -L https://github.com/signalfx/splunk-otel-java/releases/latest/download/splunk-otel-javaagent-all.jar \
    -o splunk-otel-javaagent.jar
$ java -javaagent:./splunk-otel-javaagent.jar -Dotel.resource.attributes=service.name=my-java-app \
    -jar target/java-agent-example-1.0-SNAPSHOT-shaded.jar https://google.com
```

> The `-javaagent` needs to be run before the `-jar` file,
> adding it as a JVM option, not as an application argument. For more
> information, see the [Oracle
> documentation](https://docs.oracle.com/javase/8/docs/technotes/tools/windows/java.html).

> :warning: Specify the agent as the only JVM agent for your application.
> Multiple agents may result in unpredictable results, broken instrumentation,
> and in some cases might crash your application.

To see the Java Agent in action with sample applications, see our
[examples](https://github.com/signalfx/tracing-examples/tree/master/opentelemetry-tracing/opentelemetry-java-tracing).

### Basic Configuration

The service name resource attribute is the only configuration option
that typically needs to be specified. You can set it by adding a `service.name`
attribute as shown in the [example above](#getting-started).

A couple other configuration options that may need to be changed or set are:

- Endpoint if not sending to a locally running Smart Agent with default
  configuration. See the [Jaeger exporter](#jaeger-exporter) section for more information.
- Environment resource attribute (example:
  `-Dotel.resource.attributes=service.name=my-service,deployment.environment=production`) to specify what
  environment the span originated from.

### Supported Java Versions

The agent works with Java runtimes version 8 and higher and supports all
JVM-based languages (for example, Clojure, Groovy, Kotlin, Scala). Supported
libraries and versions are listed
[here](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/supported-libraries.md).

## Advanced Configuration

> For the majority of users, the [Getting Started](#getting-started) section is
> all you need. The follow section contains advanced configuration options.

The agent can be configured in the following ways:

* System property (example: `-Dotel.resource.attributes=service.name=my-java-app`)
* Environment variable (example: `export OTEL_RESOURCE_ATTRIBUTES=service.name=my-java-app`)

> System property values take priority over corresponding environment variables.

Below you will find all the configuration options supported by this distribution.

### Jaeger exporter

| System property                   | Environment variable              | Description                                                                                                         |
|-----------------------------------|-----------------------------------|---------------------------------------------------------------------------------------------------------------------|
| otel.traces.exporter | OTEL_TRACES_EXPORTER              | Select the span exporter to use. `jaeger-thrift-splunk` is the default value.
| otel.exporter.jaeger.endpoint     | OTEL_EXPORTER_JAEGER_ENDPOINT     | The Jaeger endpoint to connect to. Default is `http://localhost:9080/v1/trace`.
| splunk.access.token               | SPLUNK_ACCESS_TOKEN               | (Optional) Auth token allowing to communicate directly with the Splunk cloud, passed as `X-SF-TOKEN` header. Default is empty. |

### Trace configuration

| System property                     | Environment variable               | Default value  | Purpose                                                                                                                                                                                                                                                                                                                                                                                                   |
| ----------------------------------- | ---------------------------------- | -------------- | ------------------------------------------------------------------------------------                                                                                                                                                                                                                                                                                                                      |
| otel.span.attribute.count.limit     | OTEL_SPAN_ATTRIBUTE_COUNT_LIMIT    | unlimited      | Maximum number of attributes per span.                                                                                                                                                                                                                                                                                                                                                                    |
| otel.span.event.count.limit         | OTEL_SPAN_EVENT_COUNT_LIMIT        | unlimited      | Maximum number of events per span.                                                                                                                                                                                                                                                                                                                                                                        |
| otel.span.link.count.limit          | OTEL_SPAN_LINK_COUNT_LIMIT         | `1000`         | Maximum number of links per span.                                                                                                                                                                                                                                                                                                                                                                         |
| otel.resource.attributes            | OTEL_RESOURCE_ATTRIBUTES           | unset          | Comma-separated list of resource attributes added to every reported span. <details><summary>Example</summary>`key1=val1,key2=val2`</details>
| otel.instrumentation.common.peer-service-mapping | OTEL_INSTRUMENTATION_COMMON_PEER_SERVICE_MAPPING | unset          | Used to add a `peer.service` attribute by specifying a comma separated list of mapping from hostnames or IP addresses. <details><summary>Example</summary>If set to `1.2.3.4=cats-service,dogs-service.serverlessapis.com=dogs-api`, requests to `1.2.3.4` will have a `peer.service` attribute of `cats-service` and requests to `dogs-service.serverlessapis.com` will have one of `dogs-api`.</details> |
| otel.instrumentation.methods.include | OTEL_INSTRUMENTATION_METHODS_INCLUDE                 | unset          | Same as adding `@WithSpan` annotation functionality for the target method string. <details><summary>Format</summary>`my.package.MyClass1[method1,method2];my.package.MyClass2[method3]`</details>                                                                                                                                                                                                            |
| otel.instrumentation.opentelemetry-annotations.exclude-methods | OTEL_INSTRUMENTATION_OPENTELEMETRY_ANNOTATIONS_EXCLUDE_METHODS | unset          | Suppress `@WithSpan` instrumentation for specific methods. <details><summary>Format</summary>`my.package.MyClass1[method1,method2];my.package.MyClass2[method3]`</details>                                                                                                                                                                                                                                |

### Java agent configuration

| System property        | Environment variable   | Default value  | Purpose                                          |
| ---------------------- | ---------------------- | -------------- | -------------------------------------------------|
| otel.javaagent.enabled | OTEL_JAVAAGENT_ENABLED | `true`         | Globally enables javaagent auto-instrumentation. |

### Splunk distribution configuration

| System property                      | Environment variable                 | Default value  | Purpose                                                  |
| ------------------------------------ | ----------------------------------   | -------------- | -------------------------------------------------------- |
| splunk.context.server-timing.enabled | SPLUNK_CONTEXT_SERVER_TIMING_ENABLED | false          | Enables adding `Server-Timing` header to HTTP responses. |

## Manually instrument a Java application

Documentation on how to manually instrument a Java application are available
[here](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/manual-instrumentation.md).

To extend the instrumentation with the OpenTelemetry Instrumentation for Java,
you have to use a compatible API version. The Splunk distribution of
OpenTelemetry Java Instrumentation version 0.9.0 is compatible with the
OpenTelemetry Instrumentation for Java version 1.0.0 and API version 1.0.0.

## Correlating traces with logs

To correlate traces with logs it is possible to add the following metadata from traces to logs:

 - Trace: `trace_id` and `span_id`
 - Resource: `service.name` and `deployment.environment`

Documentation on how to inject trace context into logs is available
[here](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/logger-mdc-instrumentation.md).

To log resource context, the Splunk distribution exposes resource attributes as
system properties prefixed with `otel.resource.` which can be used in logger
configuration.

Example configuration for log4j pattern:

```xml
<PatternLayout>
  <pattern>service: ${sys:otel.resource.service.name}, env: ${sys:otel.resource.environment} %m%n</pattern>
</PatternLayout>
```

or logback pattern:

```xml
<pattern>service: %property{otel.resource.service.name}, env: %property{otel.resource.environment}: %m%n</pattern>
```

## Troubleshooting

To turn on the agent's internal debug logging:

`-Dotel.javaagent.debug=true`

> :warning: Debug logging is extremely verbose and resource intensive. Enable
> debug logging only when needed and disable when done.

# License and versioning

The Splunk distribution of OpenTelemetry Java Instrumentation is a distribution
of the [OpenTelemetry Java Instrumentation
project](https://github.com/open-telemetry/opentelemetry-java-instrumentation).
It is released under the terms of the Apache Software License version 2.0. See
[the license file](./LICENSE) for more details.
