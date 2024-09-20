---

<p align="center">
  <strong>
    <a href="#get-started">Get Started</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="CONTRIBUTING.md">Get Involved</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="https://quickdraw.splunk.com/redirect/?product=Observability&version=current&location=java.otel.repo.migration">Migrate from SignalFx Java Agent</a>
  </strong>
</p>

<p align="center">
  <img alt="Stable" src="https://img.shields.io/badge/status-stable-informational?style=for-the-badge">
  <a href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v1.33.6">
    <img alt="OpenTelemetry Instrumentation for Java Version" src="https://img.shields.io/badge/otel-1.33.6-blueviolet?style=for-the-badge">
  </a>
  <a href="https://github.com/signalfx/gdi-specification/releases/tag/v1.6.0">
    <img alt="Splunk GDI specification" src="https://img.shields.io/badge/GDI-1.6.0-blueviolet?style=for-the-badge">
  </a>
  <a href="https://github.com/signalfx/splunk-otel-java/releases">
    <img alt="GitHub release (latest SemVer)" src="https://img.shields.io/github/v/release/signalfx/splunk-otel-java?include_prereleases&style=for-the-badge">
  </a>
  <a href="https://maven-badges.herokuapp.com/maven-central/com.splunk/splunk-otel-javaagent">
    <img alt="Maven Central" src="https://img.shields.io/maven-central/v/com.splunk/splunk-otel-javaagent?style=for-the-badge">
  </a>
  <a href="https://github.com/signalfx/splunk-otel-java/actions/workflows/ci.yaml">
    <img alt="Build Status" src="https://img.shields.io/github/actions/workflow/status/signalfx/splunk-otel-java/ci.yaml?branch=main&style=for-the-badge">
  </a>
</p>

<p align="center">
  <strong>
    <a href="https://github.com/signalfx/tracing-examples/tree/main/opentelemetry-tracing/opentelemetry-java-tracing">Examples</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="https://quickdraw.splunk.com/redirect/?product=Observability&version=current&location=java.gdi.about">About the distribution</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="SECURITY.md">Security</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="https://quickdraw.splunk.com/redirect/?product=Observability&version=current&location=java.gdi.requirements">Supported Libraries</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="https://quickdraw.splunk.com/redirect/?product=Observability&version=current&location=java.gdi.troubleshooting">Troubleshooting</a>
  </strong>
</p>

# Splunk Distribution of OpenTelemetry Java

The Splunk Distribution of [OpenTelemetry Instrumentation for
Java](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
provides a Java Virtual Machine (JVM)
agent that automatically instruments your Java application to capture and report
distributed traces to [Splunk APM](https://quickdraw.splunk.com/redirect/?product=Observability&version=current&location=apm-intro).

This distribution comes with the following defaults:

- [W3C `tracecontext`](https://www.w3.org/TR/trace-context/) and [W3C
  baggage](https://www.w3.org/TR/baggage/) context propagation;
  [B3](https://github.com/openzipkin/b3-propagation) can also be
  [configured](https://github.com/signalfx/splunk-otel-java/blob/main/docs/advanced-config.md#trace-propagation-configuration).
- [OTLP gRPC
  exporter](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/protocol/README.md)
  configured to send spans to a locally running [Splunk OpenTelemetry
  Collector](https://github.com/signalfx/splunk-otel-collector)
- Unlimited default limits for [configuration
  options](docs/advanced-config.md#trace-configuration) to support
  full-fidelity traces.

If you're currently using the SignalFx Java Agent and want to
migrate to the Splunk Distribution of OpenTelemetry Java,
see [Migrate from the SignalFx Java Agent](https://quickdraw.splunk.com/redirect/?product=Observability&version=current&location=java.otel.repo.migration).

---

<!-- Comments, spacing, empty and new lines in the section below are intentional, please do not modify them! -->
<!--DEV_DOCS_WARNING-->

## Requirements

The agent works with Java runtimes version 8 and higher. For the full list of requirements and supported libraries and versions, see [Requirements for the Java agent](https://quickdraw.splunk.com/redirect/?product=Observability&version=current&location=java.gdi.requirements) in the official Splunk documentation.

## Get started

For complete instructions on how to get started with the Splunk Distribution of OpenTelemetry Java, see [Instrument Java services for Observability Cloud](https://quickdraw.splunk.com/redirect/?product=Observability&version=current&location=learnmore.java.gdi) in the official Splunk documentation.

To see the Java Agent in action with sample applications, see our
[examples](https://github.com/signalfx/tracing-examples/tree/main/opentelemetry-tracing/opentelemetry-java-tracing).

## Advanced configuration

To fully configure the agent of the Splunk Distribution of OpenTelemetry Java, see [Configure the Java agent](https://quickdraw.splunk.com/redirect/?product=Observability&version=current&location=java.gdi.settings) in the official Splunk documentation.

## Correlating traces with logs

The Splunk Distribution of OpenTelemetry Java provides a way to correlate traces with logs. For more information see [Connect Java application trace data with logs](https://quickdraw.splunk.com/redirect/?product=Observability&version=current&location=java.gdi.correlate) 
in the Splunk Observability Cloud user documentation.

## Manually instrument a Java application

Documentation on how to manually instrument a Java application is available in the 
[OpenTelemetry official documentation](https://opentelemetry.io/docs/instrumentation/java/manual/).
To learn how to add custom metrics to your application see [Manual instrumentation](https://quickdraw.splunk.com/redirect/?product=Observability&version=current&location=java.gdi.custom.metrics).

To extend the instrumentation with the OpenTelemetry Instrumentation for Java,
you have to use a compatible API version.

<!-- IMPORTANT: do not change comments or break those lines below -->
The Splunk Distribution of OpenTelemetry Java version <!--SPLUNK_VERSION-->1.32.5<!--SPLUNK_VERSION--> is compatible
with:

* OpenTelemetry API version <!--OTEL_VERSION-->1.41.0<!--OTEL_VERSION-->
* OpenTelemetry Instrumentation for Java version <!--OTEL_INSTRUMENTATION_VERSION-->1.33.6<!--OTEL_INSTRUMENTATION_VERSION-->
* Micrometer version 1.12.2

## Snapshot builds

We publish [snapshot builds](https://oss.sonatype.org/content/repositories/snapshots/com/splunk/splunk-otel-javaagent/1.30.0-SNAPSHOT/)
with every merge to the `main` branch. Snapshots are primarily intended to test new functionality and are not recommended
for production use.

## Upgrades

For information and best practices around upgrades, see the [Upgrading documentation](https://quickdraw.splunk.com/redirect/?product=Observability&version=current&location=java.gdi.upgrades).

## Troubleshooting

For troubleshooting information and known issues, see [Troubleshooting Java instrumentation](https://quickdraw.splunk.com/redirect/?product=Observability&version=current&location=java.gdi.troubleshooting) 
in the Splunk Observability Cloud user documentation.

# License

The Splunk Distribution of OpenTelemetry Java is a distribution of [OpenTelemetry Instrumentation for Java](https://github.com/open-telemetry/opentelemetry-java-instrumentation). It is licensed under the terms of the Apache Software License version 2.0. For more details, see [the license file](./LICENSE).

>ℹ️&nbsp;&nbsp;SignalFx was acquired by Splunk in October 2019. See [Splunk SignalFx](https://www.splunk.com/en_us/investor-relations/acquisitions/signalfx.html) for more information.
