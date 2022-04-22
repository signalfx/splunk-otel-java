---

<p align="center">
  <strong>
    <a href="#get-started">Get Started</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="CONTRIBUTING.md">Get Involved</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="https://docs.splunk.com/Observability/gdi/get-data-in/application/java/troubleshooting/migrate-signalfx-java-agent-to-otel.html">Migrate from SignalFx Java Agent</a>
  </strong>
</p>

<p align="center">
  <img alt="Stable" src="https://img.shields.io/badge/status-stable-informational?style=for-the-badge">
  <a href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v1.13.0">
    <img alt="OpenTelemetry Instrumentation for Java Version" src="https://img.shields.io/badge/otel-1.13.0-blueviolet?style=for-the-badge">
  </a>
  <a href="https://github.com/signalfx/gdi-specification/releases/tag/v1.2.0">
    <img alt="Splunk GDI specification" src="https://img.shields.io/badge/GDI-1.2.0-blueviolet?style=for-the-badge">
  </a>
  <a href="https://github.com/signalfx/splunk-otel-java/releases">
    <img alt="GitHub release (latest SemVer)" src="https://img.shields.io/github/v/release/signalfx/splunk-otel-java?include_prereleases&style=for-the-badge">
  </a>
  <a href="https://maven-badges.herokuapp.com/maven-central/com.splunk/splunk-otel-javaagent">
    <img alt="Maven Central" src="https://img.shields.io/maven-central/v/com.splunk/splunk-otel-javaagent?style=for-the-badge">
  </a>
  <a href="https://github.com/signalfx/splunk-otel-java/actions?query=workflow%3A%22CI+build%22">
    <img alt="Build Status" src="https://img.shields.io/github/workflow/status/signalfx/splunk-otel-java/CI%20build?style=for-the-badge">
  </a>
</p>

<p align="center">
  <strong>
    <a href="https://github.com/signalfx/tracing-examples/tree/main/opentelemetry-tracing/opentelemetry-java-tracing">Examples</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="https://docs.splunk.com/Observability/gdi/get-data-in/application/java/splunk-java-otel-distribution.html">About the distribution</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="SECURITY.md">Security</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="https://docs.splunk.com/Observability/gdi/get-data-in/application/java/java-otel-requirements.html">Supported Libraries</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="https://docs.splunk.com/Observability/gdi/get-data-in/application/java/troubleshooting/common-java-troubleshooting.html">Troubleshooting</a>
  </strong>
</p>

# Splunk Distribution of OpenTelemetry Java

The Splunk Distribution of [OpenTelemetry Instrumentation for
Java](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
provides a [Java Virtual Machine (JVM)
agent](https://docs.oracle.com/javase/7/docs/api/java/lang/instrument/package-summary.html)
that automatically instruments your Java application to capture and report
distributed traces to [Splunk APM](https://docs.splunk.com/Observability/apm/intro-to-apm.html).

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
see [Migrate from the SignalFx Java Agent](https://docs.splunk.com/Observability/gdi/get-data-in/application/java/troubleshooting/migrate-signalfx-java-agent-to-otel.html).

---

<!-- Comments, spacing, empty and new lines in the section below are intentional, please do not modify them! -->
<!--DEV_DOCS_WARNING-->
<!--DEV_DOCS_WARNING_START-->
The following documentation refers to the in-development version of `splunk-otel-java`. Docs for the latest version ([v1.10.0](https://github.com/signalfx/splunk-otel-java/releases/latest)) can be found [here](https://github.com/signalfx/splunk-otel-java/blob/v1.10.0/README.md).

---
<!--DEV_DOCS_WARNING_END-->

## Requirements

The agent works with Java runtimes version 8 and higher. For the full list of requirements and supported libraries and versions, see [Requirements for the Java agent](https://docs.splunk.com/Observability/gdi/get-data-in/application/java/java-otel-requirements.html) in the official Splunk documentation.

## Get started

Follow these steps to automatically instrument your application using the Java agent:

1.  Check that you meet the [requirements](#requirements).

2.  Make sure that the collector you set up to receive trace data is installed and configured.

3.  Download the JAR file for the latest version of the agent:

    -   On Linux, run:

        ```bash
        curl -L https://github.com/signalfx/splunk-otel-java/releases/latest/download/splunk-otel-javaagent.jar \
        -o splunk-otel-javaagent.jar
        ```

    -   On Windows (PowerShell), run:

        ```powershell
        Invoke-WebRequest -Uri https://github.com/signalfx/splunk-otel-java/releases/latest/download/splunk-otel-javaagent.jar -OutFile splunk-otel-javaagent.jar
        ```

4.  Set the `OTEL_SERVICE_NAME` environment variable:

    -   On Linux, run:

        ```bash
        export OTEL_SERVICE_NAME=<yourServiceName>
        ```

    -   On Windows (PowerShell), run:

        ```powershell
        $env:OTEL_SERVICE_NAME=<yourServiceName>
        ```

5.  Enable the Java agent:

    ```bash
    java -javaagent:./splunk-otel-javaagent.jar \
      -jar <myapp>.jar
    ```

    Insert the `-javaagent` flag before the `-jar` file, adding it as a JVM option, not as an application argument.

> **Note**: Supported java servers may require different steps to add the path to the JVM agent to their configuration. See [Add the JVM agent to Java servers](./docs/server-instructions.md).

> **Tip**: To generate a snippet that includes all the basic install commands for your environment and service, open the Splunk Observability Cloud wizard in **Data Setup > APM Instrumentation > Java > Add Connection**.

When you run your application with the Java agent, trace data goes to Observability Cloud through the Splunk OTel connector. If no data
appears in **Observability > APM**, see [Troubleshooting](#troubleshooting).

For full install instructions, including K8s and Lambda snippets, see [Instrument Java services for Observability Cloud](https://docs.splunk.com/Observability/gdi/get-data-in/application/java/get-started.html) in the official Splunk documentation.

To see the Java Agent in action with sample applications, see our
[examples](https://github.com/signalfx/tracing-examples/tree/main/opentelemetry-tracing/opentelemetry-java-tracing).

## Advanced configuration

To fully configure the agent of the Splunk Distribution of OpenTelemetry Java, see [Configure the Java agent](https://docs.splunk.com/Observability/gdi/get-data-in/application/java/configuration/advanced-java-otel-configuration.html) in the official Splunk documentation.

## Correlating traces with logs

The Splunk Distribution of OpenTelemetry Java provides a way to correlate traces with logs. For more information see [Connect Java application trace data with logs](https://docs.splunk.com/Observability/gdi/get-data-in/application/java/instrumentation/connect-traces-logs.html) in the official Splunk documentation.

## Manually instrument a Java application

Documentation on how to manually instrument a Java application is available in the 
[OpenTelemetry official documentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/manual-instrumentation.md).
To learn how to add custom metrics to your application please consult [our docs](docs/metrics.md#manual-instrumentation).

To extend the instrumentation with the OpenTelemetry Instrumentation for Java,
you have to use a compatible API version.

<!-- IMPORTANT: do not change comments or break those lines below -->
The Splunk Distribution of OpenTelemetry Java version <!--SPLUNK_VERSION-->1.10.0<!--SPLUNK_VERSION--> is compatible
with:

* OpenTelemetry API version <!--OTEL_VERSION-->1.13.0<!--OTEL_VERSION-->
* OpenTelemetry Instrumentation for Java version <!--OTEL_INSTRUMENTATION_VERSION-->1.13.0<!--OTEL_INSTRUMENTATION_VERSION-->
* Micrometer version 1.8.5

## Snapshot builds

We publish [snapshot builds](https://oss.sonatype.org/content/repositories/snapshots/com/splunk/splunk-otel-javaagent/1.11.0-SNAPSHOT/)
with every merge to the `main` branch. Snapshots are primarily intended to test new functionality and are not recommended
for production use.

## Troubleshooting

For troubleshooting information and known issues, see [Troubleshooting Java instrumentation](https://docs.splunk.com/Observability/gdi/get-data-in/application/java/troubleshooting/common-java-troubleshooting.html) in the official Splunk documentation.

# License and versioning

The Splunk Distribution of OpenTelemetry Java is a distribution of the [OpenTelemetry Instrumentation for Java project](https://github.com/open-telemetry/opentelemetry-java-instrumentation). It is released under the terms of the Apache Software License version 2.0. For more details, see [the license file](./LICENSE).

>ℹ️&nbsp;&nbsp;SignalFx was acquired by Splunk in October 2019. See [Splunk SignalFx](https://www.splunk.com/en_us/investor-relations/acquisitions/signalfx.html) for more information.
