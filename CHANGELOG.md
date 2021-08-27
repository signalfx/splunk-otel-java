# Changelog

All notable changes to this project will be documented in this file.

The format is based on
the [Splunk GDI specification](https://github.com/signalfx/gdi-specification/blob/v1.0.0/specification/repository.md),
and this repository adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

## v1.3.1 - 2021-08-27

- OpenTelemetry Instrumentation for Java has been updated to version 1.5.2. This fixes
  the [memory leak](https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/3962) bug that
  manifested on 1.5.0 and 1.5.1 when OpenTelemetry metrics exporter wasn't used.

## v1.3.0 - 2021-08-23

### General

- OpenTelemetry Java SDK and OpenTelemetry Instrumentation for Java dependencies have been updated to version 1.5.0.

### Enhancements

- Middleware attributes (`middleware.name` and `middleware.version`) have been renamed
  to [webengine](docs/webengine-attributes.md) attributes (`webengine.name` and `webengine.version`) to follow
  the [OpenTelemetry specification](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/resource/semantic_conventions/webengine.md)
  .
- We have added instrumentations for [c3p0](https://www.mchange.com/projects/c3p0/)
  and [Vibur DBCP](https://github.com/vibur/vibur-dbcp) connection pools. The agent now collects and exports metrics for
  both JDBC connection pools.
- We have also introduced instrumentations for [Tomcat connector](https://tomcat.apache.org/tomcat-8.5-doc/index.html)
  and [WebSphere Liberty web request](https://www.ibm.com/docs/en/was-liberty/base?topic=10-threadpool-monitoring)
  thread pools. The agent now collects and exports metrics for these application server thread pools.
- This release introduces the Micrometer bridge instrumentation. You can now use the Micrometer API inside your
  application to manually define custom metrics and the javaagent will export them.
  See [the documentation](docs/metrics.md) for more details.
- The `splunk.metrics.export.interval` configuration property will now allow specifying time units; and if no units are
  specified then the value is treated as number of milliseconds. For example `30s` means "30 seconds" and is equivalent
  to `30000`.
- This release also introduces
  the [muzzle](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/contributing/muzzle.md)
  safety checks to all Splunk instrumentations. Our instrumentations now offer exactly the same level
  of [safety](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/safety-mechanisms.md)
  as the upstream OpenTelemetry instrumentations.

## v1.2.0 - 2021-07-26

### General

- OpenTelemetry Java SDK and OpenTelemetry Instrumentation for Java dependencies have been updated to version 1.4.0.

### Enhancements

- We have added instrumentation for [HikariCP](https://github.com/brettwooldridge/HikariCP)
  and [Tomcat JDBC](https://tomcat.apache.org/tomcat-8.5-doc/jdbc-pool.html) connection pools. The agent now collects
  and exports metrics for both JDBC connection pools.
- You can now set the service name using the `OTEL_SERVICE_NAME` environment variable and the `otel.service.name` system
  property (see
  the [OpenTelemetry specification](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/sdk-environment-variables.md#general-sdk-configuration)
  . This removes the need of using `OTEL_RESOURCE_ATTRIBUTES` to set the service name.

## v1.1.0 - 2021-06-18

### General

- OpenTelemetry Java SDK and OpenTelemetry Instrumentation for Java dependencies were updated to version 1.3.0.
- [khttp](https://khttp.readthedocs.io/) instrumentation was moved from the upstream OpenTelemetry Instrumentation for
  Java repo to this one. We are now responsible for maintaining this instrumentation.

### Bugfixes

- The agent will always set exactly one `Server-Timing` header value; the bug where multiple copies of the same header
  value were set was fixed.

### Enhancements

- Added `internal_root_off` sampler that will drop all traces that start with `INTERNAL`, `CLIENT` or `PRODUCER` span -
  the only top-level span kinds allowed are `SERVER` and `CONSUMER`. You can use this sampler by setting
  the `otel.traces.sampler` configuration property.

## v1.0.0 - 2021-06-02

### General

- First stable release of the Splunk Distribution of OpenTelemetry Java.

## v0.12.0 (RC) - 2021-05-24

### Breaking Changes

- The [metrics](docs/metrics.md) component will now be turned off by default. You can re-enable it by setting
  `SPLUNK_METRICS_ENABLED` to `true`.

### Enhancements

- The agent will now set a resource attribute `splunk.distro.version` with its own version.
- The CloudFoundry buildpack file for Splunk Distribution of OpenTelemetry Java will now be built as a part of the
  release process and will be attached to the GitHub release.

## v0.11.0 - 2021-05-17

### General Notes

- OpenTelemetry Java SDK and OpenTelemetry Instrumentation for Java dependencies were updated to version 1.2.0.
- Micrometer dependency was updated to version 1.7.0.

### Breaking Changes

- The Splunk Distribution of OpenTelemetry Java now uses the OTLP span exporter as the default. The OTLP exporter
  supports the `splunk.access.token` configuration option and can be used to send spans directly to Splunk cloud. The
  default OTLP exporter endpoint is `http://localhost:4317`. You can still use the Jaeger exporter by
  setting `OTEL_TRACES_EXPORTER=jaeger-thrift-splunk`.
- We have also changed the default endpoint of the SignalFx metrics exporter: it now points to `http://localhost:9943`,
  which is the default endpoint of [Splunk OpenTelemetry Connector](https://github.com/signalfx/splunk-otel-collector)
  deployed on `localhost`.
- The agent now uses [W3C `tracecontext`](https://www.w3.org/TR/trace-context/) as the default trace propagation
  mechanism; [W3C Baggage](https://w3c.github.io/baggage/) is also enabled by default. You can switch to the previous B3
  propagator by setting `OTEL_PROPAGATORS=b3multi`.
- Deprecated configuration property `splunk.context.server-timing.enabled` has been removed. You can use
  `splunk.trace-response-header.enabled` instead.

### Enhancements

- The agent will now log a warning when the `service.name` resource attribute is not provided.

## v0.10.0 - 2021-04-14

### General Notes

- Updated OpenTelemetry and OpenTelemetry Instrumentation for Java versions to 1.1.0.

### Enhancements

- The Splunk Distribution of OpenTelemetry Java now gathers basic application and JVM metrics. By default, this feature
  is enabled and is sending metrics to a SmartAgent instance running on localhost:
  you can change the endpoint
  by [setting an appropriate configuration option](docs/advanced-config.md#splunk-distribution-configuration). You can
  find a more detailed explanation about the feature [here](docs/metrics.md).
- Add metrics for JDBC connection pool: Apache Commons DBCP2. Aside from JVM metrics mentioned above, the javaagent also
  collects Apache Commons DBCP2 connection pool metrics now.
- Recommend using `deployment.environment` resource attribute instead of `environment`.
- The SignalFx Java Agent migration guide has been brought up-to-date.
- The repo README and documentation was significantly revamped. You'll now find much more information about the features
  and configuration of this project. We've also added a FAQ and troubleshooting sections that aim to answer the most
  common problems that may arise while using the Splunk Distribution of OpenTelemetry Java.

### Breaking Changes

- Removed the deprecated `signalfx.auth.token` configuration property: it was replaced by `splunk.access.token` in the
  previous release.

### Deprecations

- Configuration property `splunk.context.server-timing.enabled` was renamed to `splunk.trace-response-header.enabled`.
  The old property name still works, but it will be removed in the next release.

## v0.9.0 - 2021-03-08

### General Notes

- Updated OpenTelemetry Instrumentation for Java versions to 1.0.0.

### Deprecations

- Property `signalfx.auth.token` has been renamed to `splunk.access.token`.

## v0.8.0 - 2021-02-12

### General Notes

- Updated OpenTelemetry Instrumentation for Java versions to 0.16.1.

### Enhancements

- Instrument Netty 4.0 to add Server-Timing header.
