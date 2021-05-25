---

<p align="center">
  <strong>
    <a href="#getting-started">Getting Started</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="CONTRIBUTING.md">Getting Involved</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="MIGRATING.md">Migrating from SignalFx Java Agent</a>
  </strong>
</p>

<p align="center">
  <img alt="Beta" src="https://img.shields.io/badge/status-beta-informational?style=for-the-badge">
  <a href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v1.2.0">
    <img alt="OpenTelemetry Instrumentation for Java Version" src="https://img.shields.io/badge/otel-1.2.0-blueviolet?style=for-the-badge">
  </a>
  <a href="https://github.com/signalfx/gdi-specification/releases/tag/v1.0.0-rc.2">
    <img alt="Splunk GDI specification" src="https://img.shields.io/badge/GDI-1.0.0--rc.2-blueviolet?style=for-the-badge">
  </a>
  <a href="https://github.com/signalfx/splunk-otel-java/releases">
    <img alt="GitHub release (latest SemVer)" src="https://img.shields.io/github/v/release/signalfx/splunk-otel-java?include_prereleases&style=for-the-badge">
  </a>
  <a href="https://github.com/signalfx/splunk-otel-java/actions?query=workflow%3A%22CI+build%22">
    <img alt="Build Status" src="https://img.shields.io/github/workflow/status/signalfx/splunk-otel-java/CI%20build?style=for-the-badge">
  </a>
</p>

<p align="center">
  <strong>
    <a href="https://github.com/signalfx/tracing-examples/tree/main/opentelemetry-tracing/opentelemetry-java-tracing">Examples</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="docs/faq.md">FAQ</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="SECURITY.md">Security</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="docs/supported-libraries.md">Supported Libraries</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="docs/troubleshooting.md">Troubleshooting</a>
  </strong>
</p>

---

# Splunk Distribution of OpenTelemetry Java

The Splunk Distribution of [OpenTelemetry Instrumentation for
Java](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
provides a [Java Virtual Machine (JVM)
agent](https://docs.oracle.com/javase/7/docs/api/java/lang/instrument/package-summary.html)
that automatically instruments your Java application to capture and report
distributed traces to Splunk APM.

If you're currently using the SignalFx Java Agent and want to
migrate to the Splunk Distribution of OpenTelemetry Java,
see [Migrate from the SignalFx Java Agent](MIGRATING.md).

This distribution comes with the following defaults:

- [W3C `tracecontext`](https://www.w3.org/TR/trace-context/) context
  propagation; [B3](https://github.com/openzipkin/b3-propagation) can also be
  [configured](https://github.com/signalfx/splunk-otel-java/blob/main/docs/advanced-config.md#trace-propagation-configuration).
- [OTLP
  exporter](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/protocol/README.md)
  configured to send spans to a locally running [Splunk OpenTelemetry
  Connector](https://github.com/signalfx/splunk-otel-collector)
  (`http://localhost:4317`); [Jaeger Thrift
  exporter](https://github.com/signalfx/splunk-otel-java/blob/main/docs/advanced-config.md#trace-exporters)
  available for [Smart Agent](https://github.com/signalfx/signalfx-agent)
  (`http://localhost:9080/v1/trace`).
- Unlimited default limits for [configuration options](docs/advanced-config.md#trace-configuration) to support
  full-fidelity traces.

> :construction: This project is currently in **BETA**. It is **officially supported** by Splunk. However, breaking changes **MAY** be introduced.

## Getting Started

To get started, download the JAR for the agent's [latest
version](https://github.com/signalfx/splunk-otel-java/releases/latest/download/splunk-otel-javaagent-all.jar):

```bash
curl -L https://github.com/signalfx/splunk-otel-java/releases/latest/download/splunk-otel-javaagent-all.jar \
     -o splunk-otel-javaagent.jar
```

Enable the Java agent by adding the `-javaagent` flag to the runtime JVM
parameters and set the `service.name`. For example, if the runtime parameters
were:

```bash
java -jar myapp.jar https://splunk.com
```

Then the runtime parameters would be updated to:

```bash
java -javaagent:./splunk-otel-javaagent.jar \
     -Dotel.resource.attributes=service.name=my-java-app \
     -jar myapp.jar https://splunk.com
```

> The `-javaagent` needs to appear before the `-jar` file,
> adding it as a JVM option, not as an application argument. For more
> information, see the [Oracle
> documentation](https://docs.oracle.com/javase/8/docs/technotes/tools/windows/java.html).

> :warning: Specify the agent as the only JVM agent for your application.
> Multiple agents may result in unpredictable results, broken instrumentation,
> and in some cases might crash your application.

To see the Java Agent in action with sample applications, see our
[examples](https://github.com/signalfx/tracing-examples/tree/main/opentelemetry-tracing/opentelemetry-java-tracing).

### Basic Configuration

The service name resource attribute is the only configuration option
that needs to be specified. You can set it by adding a `service.name`
attribute as shown in the [example above](#getting-started).

A couple other configuration options that may need to be changed or set are:

- Endpoint if not sending to a locally running Splunk OpenTelemetry Connector
  with default configuration. See the
  [exporters](docs/advanced-config.md#trace-exporters) configuration
  documentation for more information.
- Environment resource attribute `deployment.environment` to specify what
  environment the span originated from. For example:
  ```
  -Dotel.resource.attributes=service.name=my-java-app,deployment.environment=production
  ```
- Service version resource attribute `service.version` to specify the version
  of your instrumented application. For example:
  ```
  -Dotel.resource.attributes=service.name=my-java-app,service.version=1.2.3
  ```

The `deployment.environment` and `service.version` resource attributes are not
strictly required by the agent, but recommended to be set if they are
available.

The `otel.resource.attributes` syntax is described in detail in the
[trace configuration](docs/advanced-config.md#trace-configuration) section.

### Supported Java Versions

The agent works with Java runtimes version 8 and higher and supports all
JVM-based languages (for example, Clojure, Groovy, Kotlin, Scala). Supported
libraries and versions are listed [here](docs/supported-libraries.md).

## Advanced Configuration

For the majority of users, the [Getting Started](#getting-started) section is
all you need. Advanced configuration documentation can be found [here](docs/advanced-config.md).

## Manually instrument a Java application

Documentation on how to manually instrument a Java application is available
[here](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/manual-instrumentation.md).

To extend the instrumentation with the OpenTelemetry Instrumentation for Java,
you have to use a compatible API version.

The Splunk Distribution of OpenTelemetry Java version 0.12.0 is compatible with:

* OpenTelemetry API version 1.2.0
* OpenTelemetry Instrumentation for Java version 1.2.0

## Correlating traces with logs

The Splunk Distribution of OpenTelemetry Java provides a way
to correlate traces with logs. It is described in detail [here](docs/correlating-traces-with-logs.md).

# License and versioning

The Splunk Distribution of OpenTelemetry Java is a distribution
of the [OpenTelemetry Instrumentation for Java
project](https://github.com/open-telemetry/opentelemetry-java-instrumentation).
It is released under the terms of the Apache Software License version 2.0. See
[the license file](./LICENSE) for more details.
