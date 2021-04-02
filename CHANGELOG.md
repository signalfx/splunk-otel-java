# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[Unreleased]: https://github.com/signalfx/splunk-otel-java/compare/v0.9.0...HEAD
[v0.9.0]: https://github.com/signalfx/splunk-otel-java/compare/v0.8.0...v0.9.0
[v0.8.0]: https://github.com/signalfx/splunk-otel-java/compare/v0.7.0...v0.8.0
