# Changelog

All notable changes to this project will be documented in this file.

The format is based on
the [Splunk GDI specification](https://github.com/signalfx/gdi-specification/blob/v1.0.0/specification/repository.md),
and this repository adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

## v1.24.1 - 2023-05-17

### Bugfixes
- Include `profiling.data.format` attribute in profiling data for GDI spec compliance (#1278)

## v1.24.0 - 2023-05-11

### General

- OpenTelemetry Java SDK has been updated to version 1.26.0.
- OpenTelemetry Instrumentation for Java has been updated to version 1.26.0.
- Micrometer dependency has been updated to version 1.11.0.

### Enhancements

- Enabled allocation event rate limiting by default in the profiler (#1225)
- Replaced the JFR event parser with the JDK mission control parser (#1229)
- Refactored the profiler so that it no longer needs to write the JFR recording into a temporary file (#1242)

### Breaking Changes

- Removed the previously deprecated profiler test formats (#1238, #1240)

## v1.23.0 - 2023-04-17

### General

- OpenTelemetry Java SDK has been updated to version 1.25.0.
- OpenTelemetry Instrumentation for Java has been updated to version 1.25.0.
- Micrometer dependency has been updated to version 1.10.6.

### Enhancements

- Prevent `splunk.realm` from configuring profiling logs to direct ingest (#1192)
- Improved compatibility with Java security manager (#1208)

### Deprecations

- Deprecate text format for `splunk.profiler.cpu.data.format` and `splunk.profiler.memory.data.format`.
  Users should migrate to the default `pprof-gzip-base64` before 1.24.0.

### Breaking Changes

- Configuration property `splunk.profiler.memory.sampler.interval` has been removed, it is replaced with a rate limiting sampler (#1182)

## v1.22.0 - 2023-03-16

### General

- OpenTelemetry Java SDK has been updated to version 1.24.0.
- OpenTelemetry Instrumentation for Java has been updated to version 1.24.0.
- GHA workflows now use Java 17 temurin
- Change scratch GHCR Docker image back to busybox for better compatibility with the k8s operator (#1155)

## v1.21.0 - 2023-02-23

### General

- OpenTelemetry Java SDK has been updated to version 1.23.1.
- OpenTelemetry Instrumentation for Java has been updated to version 1.21.0.
- Micrometer dependency has been updated to version 1.10.4.
- [signalfx-java](https://github.com/signalfx/signalfx-java) dependency has been updated to version 1.0.29.

### Enhancements

- Starting with this release we will publish a GHCR-hosted Docker image that contains the javaagent jar. (#1108)

## v1.20.0 - 2023-01-18

Regular maintenance release, coordinated after the upstream/vanilla release.

### General

- OpenTelemetry Java SDK updated to version 1.22.0
- OpenTelemetry Instrumentation for Java dependencies updated to version 1.22.1.
- Upgrade to Gradle 7.6 (#1062)
- Upgrade to Micrometer 1.10.3 (#1065)
- Truncate `process.command_line` resource attribute when metrics are enabled (#1057) 
- Numerous other minor dependency upgrades. 

## v1.19.0 - 2022-12-16

### General

- OpenTelemetry Java SDK and OpenTelemetry Instrumentation for Java dependencies have been updated to version 1.21.0. (#1029)
- Micrometer dependency has been updated to version 1.10.2. (#1018)
- Comply with [GDI spec version 1.4.0](https://github.com/signalfx/gdi-specification/releases/tag/v1.4.0)

## v1.18.0 - 2022-11-23

### Enhancements

- Profiler checks for storage writability on startup, and logs an error and disables profiling if it is not. (#984)

### General

- OpenTelemetry Java SDK and OpenTelemetry Instrumentation for Java dependencies have been updated to versions 1.20.1 and 1.20.2 respectively.
- Micrometer dependency has been updated to version 1.10.1. (#1001)
- Protobuf dependency has been updated to version 3.21.9, which mitigates CVE-2022-3171. (#978)
- Service name detection moved to upstream OpenTelemetry Instrumentation.
- [signalfx-java](https://github.com/signalfx/signalfx-java) dependency has been updated to version 1.0.26.

## v1.17.0 - 2022-10-19

### Deprecations
- Jaeger Thrift exporter has been deprecated and a warning is logged if it is used. (#932). Users are strongly encouraged to use OTLP when sending data to jaeger.

### Enhancements
- Profiler now defaults JFR storage to system temp directory (#943)
- Service name is detected from jar file when otherwise not specified (#925)

### General

- OpenTelemetry Java SDK and OpenTelemetry Instrumentation for Java dependencies have been updated to version 1.19.1.
- Micrometer dependency has been updated to version 1.9.5. (#947)
- Update to [signalfx-java](https://github.com/signalfx/signalfx-java) 1.0.25, which updates `jackson-databind` and mitigates GHSA-jjjh-jjxp-wpff and GHSA-rgv9-q543-rqg4 (#968)

## v1.16.0 - 2022-09-15

### General

- OpenTelemetry Java SDK and OpenTelemetry Instrumentation for Java dependencies have been updated to version 1.18.0.
- Micrometer dependency has been updated to version 1.9.4.

### Enhancements
- Implemented automatic service name detection for servlet applications. The agent will now look for
  the value of the top-level `display-name` tag in the `web.xml` file and use it as
  the `service.name` resource attribute, in case the user hasn't provided it.

## v1.15.0 - 2022-08-22

### General

- OpenTelemetry Java SDK and OpenTelemetry Instrumentation for Java dependencies have been updated to version 1.17.0.
- Micrometer dependency has been updated to version 1.9.3.

## v1.14.2 - 2022-08-11

- Fix allocated memory metrics by preventing WeakReference from GCing (#864)

## v1.14.1 - 2022-07-27

### Bugfixes
- Metrics are once again enabled when memory profiling is turned on (#857)

## v1.14.0 - 2022-07-22

### Bugfixes
- Exclude transitive gson dependency (mitigates [CVE-2022-25647](https://nvd.nist.gov/vuln/detail/CVE-2022-25647)) (#829)

### Enhancements
- Add `splunk.metrics.implementation` config property to allow switching metrics implementation (#836)
- Several instrumentation additions, including C3P0 connection pool metrics, Kafka client metrics, JVM buffer pool metrics, and more!
  - See the upstream [1.16.0 release notes](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v1.16.0) for additional details.

### General

- OpenTelemetry Java SDK and OpenTelemetry Instrumentation for Java dependencies have been updated to version 1.16.0.
- Micrometer dependency has been updated to version 1.9.2.

## v1.13.1 - 2022-07-04

### General

- SignalFx metrics exporter has been updated to version 1.0.20.
- jaeger-client has been updated to version 1.8.1 (#833)
- gson dependency has been excluded from agent (#829)

## v1.13.0 - 2022-06-17

### General

- OpenTelemetry Java SDK and OpenTelemetry Instrumentation for Java dependencies have been updated to version 1.15.0.
- Micrometer dependency has been updated to version 1.9.1.

### Breaking Changes

- Deprecated configuration property `splunk.profiler.period.{eventName}` has been removed. You can use
  `splunk.profiler.call.stack.interval` instead.

## v1.12.0 - 2022-06-09

### General

- Compliance with [GDI spec version 1.3.0](https://github.com/signalfx/gdi-specification/releases/tag/v1.3.0)
- Change metric names used by memory profiling:
  - `jvm.experimental.memory.allocated` -> `process.runtime.jvm.memory.allocated`
  - `jvm.experimental.memory.reclaimed` -> `process.runtime.jvm.memory.reclaimed`
    (these two have also been migrated from Gauge to Counter)

### Bugfixes

- Fixed `OTEL_EXPORTER_OTLP_ENDPOINT` not overriding the `splunk.realm` property (#795)

### Enhancements

- Profiling stack trace data defaults to pprof format (#799)

## v1.11.0 - 2022-05-18

### General

- OpenTelemetry Java SDK and OpenTelemetry Instrumentation for Java dependencies have been updated to version 1.14.0.
- Micrometer dependency has been updated to version 1.9.0.
- SignalFx metrics exporter has been updated to version 1.0.19.

### Bugfixes

- Fixed a bug where multiple Profiler instances on a single host would generate temporary files with the same name.

### Enhancements

- Implemented support for the `splunk.realm` configuration property, which allows easy configuration of exporter
  endpoints when sending data directly to the Splunk cloud.
- Implemented internal stack trace filtering for the allocation profiler.

## v1.10.2 - 2022-05-12

### General

- SignalFx metrics exporter has been updated to version 1.0.18.

## v1.10.1 - 2022-04-26

### General

- OpenTelemetry Instrumentation for Java has been updated to version 1.13.1.

## v1.10.0 - 2022-04-22

### General

- OpenTelemetry Java SDK and OpenTelemetry Instrumentation for Java dependencies have been updated to version 1.13.0.
- Micrometer dependency has been updated to version 1.8.5.

### Bugfixes

- Fix `NullPointerException` on context tracking when JFR event on JDK 17 provides no thread - [#743](https://github.com/signalfx/splunk-otel-java/pull/743)

### Enhancements

- Added `host.name` and `container.id` tags to Micrometer metrics - [#726](https://github.com/signalfx/splunk-otel-java/pull/726)
- Added support for exporting events in profiler in pprof format which can be enabled using the `splunk.profiler.cpu.data.format` setting - [#684](https://github.com/signalfx/splunk-otel-java/pull/684)
- Added profiler setting `splunk.profiler.max.stack.depth` to limit maximum depth of exported stack traces - [#739](https://github.com/signalfx/splunk-otel-java/pull/739)

## v1.9.1 - 2022-03-18

### General

- OpenTelemetry Instrumentation for Java has been updated to version 1.12.1.

### Bugfixes

- Fixed Elasticsearch rest client using high cardinality span name.
- Fixed a possible deadlock.

## v1.9.0 - 2022-03-14

### General

- OpenTelemetry Instrumentation for Java has been updated to version 1.12.0.

### Bugfixes

- Fix servlet instrumentation to prevent overwriting `Server-Timing` header for internal spans - [#694](https://github.com/signalfx/splunk-otel-java/pull/694)

### Enhancements

- `JvmHeapPressureMetrics` are now part of exported JVM metrics - [#686](https://github.com/signalfx/splunk-otel-java/pull/686).
- Size reduction for call stack payloads for profiling TLAB events - [#687](https://github.com/signalfx/splunk-otel-java/pull/687)

## v1.8.2 - 2022-02-22

### Bugfixes

- Fixed exporting histograms with fixed buckets.

## v1.8.1 - 2022-02-21

### General

- OpenTelemetry Instrumentation for Java has been updated to version 1.11.1.

### Bugfixes

- Fixed regression in loading the Prometheus exporter.

## v1.8.0 - 2022-02-15

### General

- OpenTelemetry Java SDK and OpenTelemetry Instrumentation for Java dependencies have been updated to version 1.11.0.

### Deprecations

- DEPRECATE `splunk-otel-javaagent-all.jar` use `splunk-otel-javaagent.jar` instead.

## v1.7.3 - 2022-02-02

### Bugfixes

- Fixed a bug that caused JFR events to appear out of order.

## v1.7.2 - 2022-01-31

### General

- OpenTelemetry Java SDK and OpenTelemetry Instrumentation for Java dependencies have been updated to version 1.10.1.

## v1.7.1 - 2022-01-18

### Bugfixes

- Fixed the docker image publishing issue in GitLab.

## v1.7.0 - 2022-01-17

### General

- Micrometer dependency has been updated to version 1.8.2.
- OpenTelemetry Java SDK and OpenTelemetry Instrumentation for Java dependencies have been updated to version 1.10.0.

### Deprecations

- DEPRECATE `splunk.profiler.period.threaddump` config setting in favor of GDI-spec
  compatible `splunk.profiler.call.stack.interval`.

### Enhancements

- Added a new `splunk.profiler.memory.enabled` property that enables all memory profiling features.
- Implemented a sampler for allocation-related profiling events. It can be enabled by setting
  the `splunk.profiler.memory.sampler.interval` property. The default value is 1; set the value to 2 or higher to sample
  data every nth allocation event.

## v1.6.1 - 2022-01-12

### General

- OpenTelemetry Instrumentation for Java has been updated to version 1.9.2.

### Bugfixes

- Fixed the connection leak in the reactor-netty upstream instrumentation.

## v1.6.0 - 2021-12-01

### General

- OpenTelemetry Java SDK and OpenTelemetry Instrumentation for Java dependencies have been updated to version 1.9.1.
- Micrometer dependency has been updated to version 1.8.0.

### Bugfixes

- Fix Tomcat thread pool metrics in Tomcat 10.
- Disabled correlation by span links in messaging instrumentations. Now the `PRODUCER`-`CONSUMER` spans will always have
  parent-child relationship.

### Enhancements

- The [Micrometer bridge instrumentation](docs/metrics.md#manual-instrumentation) now supports versions starting from
  1.3.0.
- A new [Oracle Universal Connection Pool](https://docs.oracle.com/database/121/JJUCP/intro.htm#JJUCP8109)
  instrumentation has been added. The agent now collects and exports metrics for Oracle UCP connection pools.
- Profiling: changed the default thread dump rate to 10 seconds (previously 1 second).

## v1.5.0 - 2021-10-20

### General

- OpenTelemetry Java SDK and OpenTelemetry Instrumentation for Java dependencies have been updated to version 1.7.0.
- Micrometer dependency has been updated to version 1.7.5.

### Bugfixes

- The Java agent no longer adds a security header to exported metrics when the `SPLUNK_ACCESS_TOKEN` is not configured.
  This means the agent should now work correctly with a collector that has token passthrough enabled.

### Enhancements

- We have added instrumentation for [WebLogic](https://docs.oracle.com/en/middleware/standalone/weblogic-server/)
  thread pools. The agent now collects and exports metrics for the WebLogic application server thread pools.
- We have also added instrumentation for Netty 4.1 that will add the [server trace](docs/server-trace-info.md) headers
  to the HTTP response.
- This release introduces the Java profiler. Keep in mind this feature is still experimental, and thus turned off by
  default; you can enable it by setting the `splunk.profiler.enabled` property to `true`. Find out more about Splunk
  profiler in its [docs](profiler/README.md).

## v1.4.0 - 2021-09-20

### General

- OpenTelemetry Java SDK and OpenTelemetry Instrumentation for Java dependencies have been updated to version 1.6.0.
- Micrometer dependency was updated to version 1.7.4.

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
  which is the default endpoint of [Splunk OpenTelemetry Collector](https://github.com/signalfx/splunk-otel-collector)
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
