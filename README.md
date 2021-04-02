---

<p align="center">
  <a href="https://github.com/signalfx/splunk-otel-java/actions?query=workflow%3A%22CI+build%22">
    <img alt="Build Status" src="https://img.shields.io/github/workflow/status/signalfx/splunk-otel-java/CI%20build?style=for-the-badge">
  </a>
  <a href="https://github.com/signalfx/splunk-otel-java/releases">
    <img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/signalfx/splunk-otel-java?include_prereleases&style=for-the-badge">
  </a>
</p>

---

# Splunk distribution of OpenTelemetry Java Instrumentation

The Splunk distribution of [OpenTelemetry Java
Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
provides a [Java Virtual Machine (JVM)
agent](https://docs.oracle.com/javase/7/docs/api/java/lang/instrument/package-summary.html)
that automatically instruments your Java application to capture and report
distributed traces to Splunk APM.

If you're currently using the SignalFx Java Agent and want to
migrate to the Splunk Distribution of OpenTelemetry Java Instrumentation,
see [Migrate from the SignalFx Java Agent](migration.md).

This Splunk distribution comes with the following defaults:

- [B3 context propagation](https://github.com/openzipkin/b3-propagation).
- [Jaeger-Thrift exporter](https://www.jaegertracing.io)
  configured to send spans to a locally running [SignalFx Smart
  Agent](https://docs.signalfx.com/en/latest/apm/apm-getting-started/apm-smart-agent.html)
  (`http://localhost:9080/v1/trace`).
- Unlimited default limits for [configuration options](docs/advanced-config.md#trace-configuration) to
  support full-fidelity traces.

> :construction: This project is currently in **BETA**.

## Getting Started

To get started, download the JAR for the agent's [latest
version](https://github.com/signalfx/splunk-otel-java/releases/latest/download/splunk-otel-javaagent-all.jar):

```bash
curl -L https://github.com/signalfx/splunk-otel-java/releases/latest/download/splunk-otel-javaagent-all.jar \
     -o splunk-otel-javaagent.jar
```

Enable the Java agent by adding the `-javaagent` flag to the runtime JVM parameters.
For example, if the runtime parameters were:

```bash
java -jar myapp.jar https://google.com
```

Then the runtime parameters would be updated to:

```bash
java -javaagent:./splunk-otel-javaagent.jar \
     -Dotel.resource.attributes=service.name=my-java-app \
     -jar myapp.jar https://google.com
```

> The `-javaagent` needs to appear before the `-jar` file,
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
that is required by the javaagent. You can set it by adding a `service.name`
attribute as shown in the [example above](#getting-started).

A couple other configuration options that may need to be changed or set are:

- Endpoint if not sending to a locally running Smart Agent with default
  configuration. See the [Jaeger exporter](docs/advanced-config.md#jaeger-exporter)
  configuration documentation for more information.
- Environment resource attribute `deployment.environment` to specify what environment
  the span originated from. For example:
  ```
  -Dotel.resource.attributes=service.name=my-java-app,deployment.environment=production
  ```

The `otel.resource.attributes` syntax is described in detail in the
[trace configuration](docs/advanced-config.md#trace-configuration) section.

### Supported Java Versions

The agent works with Java runtimes version 8 and higher and supports all
JVM-based languages (for example, Clojure, Groovy, Kotlin, Scala). Supported
libraries and versions are listed
[here](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/supported-libraries.md).

## Advanced Configuration

For the majority of users, the [Getting Started](#getting-started) section is
all you need. Advanced configuration documentation can be found [here](docs/advanced-config.md).

## Manually instrument a Java application

Documentation on how to manually instrument a Java application are available
[here](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/manual-instrumentation.md).

To extend the instrumentation with the OpenTelemetry Instrumentation for Java,
you have to use a compatible API version.

The Splunk distribution of OpenTelemetry Java Instrumentation version 0.9.0 is compatible with:

* OpenTelemetry API version 1.0.0
* OpenTelemetry Instrumentation for Java version 1.0.0

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
