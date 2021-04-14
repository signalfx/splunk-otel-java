# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [v0.10.0]

### Added

- Implement gathering basic JVM metrics with micrometer.
  [#135](https://github.com/signalfx/splunk-otel-java/pull/135),
  [#136](https://github.com/signalfx/splunk-otel-java/pull/136)
- Add metrics for JDBC connection pool: Apache Commons DBCP2.
  [#144](https://github.com/signalfx/splunk-otel-java/pull/144),
  [#146](https://github.com/signalfx/splunk-otel-java/pull/146),
  [#148](https://github.com/signalfx/splunk-otel-java/pull/148)

### Changed

- Recommend using `deployment.environment` resource attribute instead of `environment`.
  [#139](https://github.com/signalfx/splunk-otel-java/pull/139)
- Updated the SignalFx Java Agent migration guide.
  [#141](https://github.com/signalfx/splunk-otel-java/pull/141),
  [#183](https://github.com/signalfx/splunk-otel-java/pull/183)
- Significantly improved the repo documentation.
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

- Renamed property `splunk.context.server-timing.enabled` to `splunk.trace-response-header.enabled`.
  [#185](https://github.com/signalfx/splunk-otel-java/pull/185)

### Removed

- Removed the deprecated `signalfx.auth.token` property: it was replaced by `splunk.access.token`.
  [#171](https://github.com/signalfx/splunk-otel-java/pull/171)

## [v0.9.0] - 2021-03-08

### Added

- Added Windows TomEE image and test. [#122](https://github.com/signalfx/splunk-otel-java/pull/122)
- Added OpenJ9 smoke test Docker images. [#127](https://github.com/signalfx/splunk-otel-java/pull/127)

### Changed

- Use upstream opentelemetry-java-instrumentation 1.0.0. [#124](https://github.com/signalfx/splunk-otel-java/pull/124)

### Deprecated

- Property `signalfx.auth.token` has been renamed to `splunk.access.token`. [#125](https://github.com/signalfx/splunk-otel-java/pull/125)

## [v0.8.0] - 2021-02-12

### Added

- Instrument Netty 4.0 to add Server-Timing header [#108](https://github.com/signalfx/splunk-otel-java/pull/108)

### Changed

- Use upstream opentelemetry-java-instrumentation 0.16.1. [#117](https://github.com/signalfx/splunk-otel-java/pull/117)

[Unreleased]: https://github.com/signalfx/splunk-otel-java/compare/v0.10.0...HEAD
[v0.10.0]: https://github.com/signalfx/splunk-otel-java/compare/v0.9.0...v0.10.0
[v0.9.0]: https://github.com/signalfx/splunk-otel-java/compare/v0.8.0...v0.9.0
[v0.8.0]: https://github.com/signalfx/splunk-otel-java/compare/v0.7.0...v0.8.0
