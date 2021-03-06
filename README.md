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
  <img alt="Stable" src="https://img.shields.io/badge/status-stable-informational?style=for-the-badge">
  <a href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v1.3.0">
    <img alt="OpenTelemetry Instrumentation for Java Version" src="https://img.shields.io/badge/otel-1.3.0-blueviolet?style=for-the-badge">
  </a>
  <a href="https://github.com/signalfx/gdi-specification/releases/tag/v1.0.0">
    <img alt="Splunk GDI specification" src="https://img.shields.io/badge/GDI-1.0.0-blueviolet?style=for-the-badge">
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

<!-- Comments, spacing, empty and new lines in the section below are intentional, please do not modify them! -->
<!--DEV_DOCS_WARNING-->
<!--DEV_DOCS_WARNING_START-->
The documentation below refers to the in development version of this package. Docs for the latest version ([v1.1.0](https://github.com/signalfx/splunk-otel-java/releases/tag/v1.1.0)) can be found [here](https://github.com/signalfx/splunk-otel-java/blob/v1.1.0/README.md).

---
<!--DEV_DOCS_WARNING_END-->

# Splunk Distribution of OpenTelemetry Java

The Splunk Distribution of [OpenTelemetry Instrumentation for
Java](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
provides a [Java Virtual Machine (JVM)
agent](https://docs.oracle.com/javase/7/docs/api/java/lang/instrument/package-summary.html)
that automatically instruments your Java application to capture and report
distributed traces to Splunk APM.

This distribution comes with the following defaults:

- [W3C `tracecontext`](https://www.w3.org/TR/trace-context/) and [W3C
  baggage](https://www.w3.org/TR/baggage/) context propagation;
  [B3](https://github.com/openzipkin/b3-propagation) can also be
  [configured](https://github.com/signalfx/splunk-otel-java/blob/main/docs/advanced-config.md#trace-propagation-configuration).
- [OTLP gRPC
  exporter](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/protocol/README.md)
  configured to send spans to a locally running [Splunk OpenTelemetry
  Connector](https://github.com/signalfx/splunk-otel-collector)
  (`http://localhost:4317`); [Jaeger Thrift
  exporter](https://github.com/signalfx/splunk-otel-java/blob/main/docs/advanced-config.md#trace-exporters)
  available for [Smart Agent](https://github.com/signalfx/signalfx-agent)
  (`http://localhost:9080/v1/trace`).
- Unlimited default limits for [configuration
  options](docs/advanced-config.md#trace-configuration) to support
  full-fidelity traces.

If you're currently using the SignalFx Java Agent and want to
migrate to the Splunk Distribution of OpenTelemetry Java,
see [Migrate from the SignalFx Java Agent](MIGRATING.md).

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
     -Dotel.service.name=my-java-app \
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

The service name is the only configuration option that is required. You can
specify it by setting the `otel.service.name` system property as shown in the
[example above](#getting-started).

A few other configuration options that may need to be changed or set are:

- Trace propagation format if not sending to other applications using W3C
  trace-context. For example, if other applications are instrumented with
  `signalfx-*-tracing` instrumentation. See the [trace
  propagation](docs/advanced-config.md#trace-propagation-configuration)
  configuration documentation for more information.
- Endpoint if not sending to a locally running Splunk OpenTelemetry Connector
  with default configuration. For example, if the SignalFx Smart Agent is used.
  See the [exporters](docs/advanced-config.md#trace-exporters) configuration
  documentation for more information.
- Environment resource attribute `deployment.environment` to specify what
  environment the span originated from. For example:
  ```
  -Dotel.resource.attributes=deployment.environment=production
  ```
- Service version resource attribute `service.version` to specify the version
  of your instrumented application. For example:
  ```
  -Dotel.resource.attributes=deployment.environment=production,service.version=1.2.3
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

<!-- IMPORTANT: do not change comments or break those lines below -->
The Splunk Distribution of OpenTelemetry Java version <!--SPLUNK_VERSION-->1.1.0<!--SPLUNK_VERSION--> is compatible
with:

* OpenTelemetry API version <!--OTEL_VERSION-->1.3.0<!--OTEL_VERSION-->
* OpenTelemetry Instrumentation for Java version <!--OTEL_INSTRUMENTATION_VERSION-->1.3.0<!--OTEL_INSTRUMENTATION_VERSION-->

## Correlating traces with logs

The Splunk Distribution of OpenTelemetry Java provides a way
to correlate traces with logs. It is described in detail [here](docs/correlating-traces-with-logs.md).

# License and versioning

The Splunk Distribution of OpenTelemetry Java is a distribution
of the [OpenTelemetry Instrumentation for Java
project](https://github.com/open-telemetry/opentelemetry-java-instrumentation).
It is released under the terms of the Apache Software License version 2.0. See
[the license file](./LICENSE) for more details.
