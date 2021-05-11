# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Breaking Changes

- The Splunk Distribution of OpenTelemetry Java Instrumentation now uses the OTLP span exporter as the default. The OTLP
  exporter supports the `splunk.access.token` configuration option and can be used to send spans directly to Splunk
  cloud. The default OTLP exporter endpoint is `http://localhost:4317`. You can still use the Jaeger exporter by
  setting `OTEL_TRACES_EXPORTER=jaeger-thrift-splunk`.
- We have also changed the default endpoint of the SignalFx metrics exporter: it now points to `http://localhost:9943`,
  which is the default endpoint of [Splunk OpenTelemetry Connector](https://github.com/signalfx/splunk-otel-collector)
  deployed on `localhost`.
- The agent now uses [W3C `tracecontext`](https://www.w3.org/TR/trace-context/) as the default trace propagation
  mechanism; [W3C Baggage](https://w3c.github.io/baggage/) is also enabled by default. You can switch to the previous B3
  propagator by setting `OTEL_PROPAGATORS=b3multi`.
- Deprecated configuration property `splunk.context.server-timing.enabled` has been removed. You can use
  `splunk.context.server-timing.enabled` instead.

### Enhancements

- The agent will now log a warning when the `service.name` resource attribute is not provided.

## [v0.10.0]

### Added

- The Splunk Distribution of OpenTelemetry Java Instrumentation now gathers basic application and JVM metrics. By
  default, this feature is enabled and is sending metrics to a localhost SmartAgent instance:
  you can change the endpoint
  by [setting an appropriate configuration option](docs/advanced-config.md#splunk-distribution-configuration). You can
  find a more detailed explanation about the feature [here](docs/metrics.md).
  [#135](https://github.com/signalfx/splunk-otel-java/pull/135),
  [#136](https://github.com/signalfx/splunk-otel-java/pull/136)
- Add metrics for JDBC connection pool: Apache Commons DBCP2. Aside from JVM metrics mentioned above, the javaagent also
  collects Apache Commons DBCP2 connection pool metrics now.
  [#144](https://github.com/signalfx/splunk-otel-java/pull/144),
  [#146](https://github.com/signalfx/splunk-otel-java/pull/146),
  [#148](https://github.com/signalfx/splunk-otel-java/pull/148)

### Changed

- Recommend using `deployment.environment` resource attribute instead of `environment`.
  [#139](https://github.com/signalfx/splunk-otel-java/pull/139)
- The SignalFx Java Agent migration guide has been brought up-to-date.
  [#141](https://github.com/signalfx/splunk-otel-java/pull/141),
  [#183](https://github.com/signalfx/splunk-otel-java/pull/183)
- The repo README and documentation was significantly revamped. You'll now find much more information about the features
  and configuration of this project. We've also added a FAQ and troubleshooting sections that aim to answer the most
  common problems that may arise while using the Splunk Distribution of OpenTelemetry Java Instrumentation.
  [#152](https://github.com/signalfx/splunk-otel-java/pull/152),
  [#155](https://github.com/signalfx/splunk-otel-java/pull/155),
  [#169](https://github.com/signalfx/splunk-otel-java/pull/169),
  [#187](https://github.com/signalfx/splunk-otel-java/pull/187),
  [#188](https://github.com/signalfx/splunk-otel-java/pull/188),
  [#191](https://github.com/signalfx/splunk-otel-java/pull/191),
  [#201](https://github.com/signalfx/splunk-otel-java/pull/201)
- Updated OpenTelemetry and OpenTelemetry Java Instrumentation versions to 1.1.0.
  [#199](https://github.com/signalfx/splunk-otel-java/pull/199)

### Deprecated

- Configuration property `splunk.context.server-timing.enabled` was renamed to `splunk.trace-response-header.enabled`.
  The old property name still works, but it will be removed in the next release.
  [#185](https://github.com/signalfx/splunk-otel-java/pull/185)

### Removed

- Removed the deprecated `signalfx.auth.token` configuration property: it was replaced by `splunk.access.token` in the
  previous release.
  [#171](https://github.com/signalfx/splunk-otel-java/pull/171)

## [v0.9.0] - 2021-03-08

### Added

- Added Windows TomEE image and test.
  [#122](https://github.com/signalfx/splunk-otel-java/pull/122)
- Added OpenJ9 smoke test Docker images.
  [#127](https://github.com/signalfx/splunk-otel-java/pull/127)

### Changed

- Use upstream opentelemetry-java-instrumentation 1.0.0.
  [#124](https://github.com/signalfx/splunk-otel-java/pull/124)

### Deprecated

- Property `signalfx.auth.token` has been renamed to `splunk.access.token`.
  [#125](https://github.com/signalfx/splunk-otel-java/pull/125)

## [v0.8.0] - 2021-02-12

### Added

- Instrument Netty 4.0 to add Server-Timing header.
  [#108](https://github.com/signalfx/splunk-otel-java/pull/108)

### Changed

- Use upstream opentelemetry-java-instrumentation 0.16.1.
  [#117](https://github.com/signalfx/splunk-otel-java/pull/117)

[Unreleased]: https://github.com/signalfx/splunk-otel-java/compare/v0.10.0...HEAD
[v0.10.0]: https://github.com/signalfx/splunk-otel-java/compare/v0.9.0...v0.10.0
[v0.9.0]: https://github.com/signalfx/splunk-otel-java/compare/v0.8.0...v0.9.0
[v0.8.0]: https://github.com/signalfx/splunk-otel-java/compare/v0.7.0...v0.8.0
