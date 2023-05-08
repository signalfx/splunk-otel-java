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
  <a href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v1.25.0">
    <img alt="OpenTelemetry Instrumentation for Java Version" src="https://img.shields.io/badge/otel-1.25.0-blueviolet?style=for-the-badge">
  </a>
  <a href="https://github.com/signalfx/gdi-specification/releases/tag/v1.5.0">
    <img alt="Splunk GDI specification" src="https://img.shields.io/badge/GDI-1.5.0-blueviolet?style=for-the-badge">
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
<!--DEV_DOCS_WARNING_START-->
The following documentation refers to the in-development version of `splunk-otel-java`. Docs for the latest version ([v1.23.0](https://github.com/signalfx/splunk-otel-java/releases/latest)) can be found [here](https://github.com/signalfx/splunk-otel-java/blob/v1.23.0/README.md).

---
<!--DEV_DOCS_WARNING_END-->

## Requirements

The agent works with Java runtimes version 8 and higher. For the full list of requirements and supported libraries and versions, see [Requirements for the Java agent](https://quickdraw.splunk.com/redirect/?product=Observability&version=current&location=java.gdi.requirements) in the official Splunk documentation.

## Get started

For complete instructions on how to get started with the Splunk Distribution of OpenTelemetry JS, see [Instrument Java services for Observability Cloud](https://quickdraw.splunk.com/redirect/?product=Observability&version=current&location=learnmore.java.gdi) in the official Splunk documentation.

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
The Splunk Distribution of OpenTelemetry Java version <!--SPLUNK_VERSION-->1.23.0<!--SPLUNK_VERSION--> is compatible
with:

* OpenTelemetry API version <!--OTEL_VERSION-->1.25.0<!--OTEL_VERSION-->
* OpenTelemetry Instrumentation for Java version <!--OTEL_INSTRUMENTATION_VERSION-->1.25.0<!--OTEL_INSTRUMENTATION_VERSION-->
* Micrometer version 1.10.4

## Snapshot builds

We publish [snapshot builds](https://oss.sonatype.org/content/repositories/snapshots/com/splunk/splunk-otel-javaagent/1.24.0-SNAPSHOT/)
with every merge to the `main` branch. Snapshots are primarily intended to test new functionality and are not recommended
for production use.

## When to upgrade?

In general, we recommend that you upgrade to each new version shortly after it is released. Ideally,
you should upgrade within a few weeks of a release. Given that OpenTelemetry is a very active 
community, consistent, frequent upgrades will help limit the number of changes between upgrades. 

Upgrades should be intentional and version numbers should be pinned in your build pipeline. See [VERSIONING.md](VERSIONING.md) for more information.

### Upgrade best practices

To reduce the risk of problems with an upgrade, do the following:

* Don't use snapshot builds in production.
* Read the release notes and changelog for each release, to help you determine when the release has
  changes that might affect your software stack. Give special consideration to specific mentions of 
  libraries, frameworks, and tools that your software uses.
* Use canary instances. Let the canaries operate with the code before releasing the code to 
  production. Run the canaries for at least a few hours, and preferably for a few days.
* Minimize the number of dependencies, including instrumentation, changing in a given release.
  It becomes more difficult to determine the root cause of a problem when multiple
  dependencies have been upgraded at the same time.

## Understanding version numbers

Refer to
the [versioning document](https://github.com/signalfx/splunk-otel-java/blob/main/VERSIONING.md) to
learn more about version numbers. Major versions contain a large number of changes, which might
result in increased risk to your production environment. The most common releases are marked with a
minor version, and they contain a modest number of changes. Patch releases are infrequent, and they
pinpoint specific fixes or enhancements.


## Troubleshooting

For troubleshooting information and known issues, see [Troubleshooting Java instrumentation](https://quickdraw.splunk.com/redirect/?product=Observability&version=current&location=java.gdi.troubleshooting) 
in the Splunk Observability Cloud user documentation.

# License

The Splunk Distribution of OpenTelemetry Java is a distribution of [OpenTelemetry Instrumentation for Java](https://github.com/open-telemetry/opentelemetry-java-instrumentation). It is licensed under the terms of the Apache Software License version 2.0. For more details, see [the license file](./LICENSE).

>ℹ️&nbsp;&nbsp;SignalFx was acquired by Splunk in October 2019. See [Splunk SignalFx](https://www.splunk.com/en_us/investor-relations/acquisitions/signalfx.html) for more information.
