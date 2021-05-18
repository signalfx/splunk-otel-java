# Splunk Distribution of OpenTelemetry Java Versioning

Splunk Distribution of OpenTelemetry Java pulls in the the OpenTelemetry Java
Instrumentation repository. As a result it cannot and will not mark anything
stable, meaning supported without breaking changes, unless OpenTelemetry has
marked it as stable or it is not specific to OpenTelemetry and is marked stable
by some other means (for example the GDI specification).

As a concrete example, the 1.0 Splunk Distribution of OpenTelemetry Java
release will apply to the tracing signal and the tracing components
OpenTelemetry marked stable in its 1.0 release as well as the components marked
as stable in the GDI specification 1.0 release. Some non-exhaustive examples of
components that are not stable in the 1.0 release, meaning breaking changes may
be introduced, include OpenTelemetry semantic conventions, automatic
instrumentation libraries, and the metric signal.

## Version numbers

The project follows [Semantic Versioning 2.0.0](https://semver.org/). This means
that all artifacts have a version of the format `MAJOR.MINOR.PATCH`.

- No backwards-incompatible changes will be made unless incrementing the `MAJOR` version number.
- Most releases will be made by incrementing the `MINOR` version.
- Patch releases will be made by incrementing the `PATCH` version.

The version number is not kept in sync with the OpenTelemetry Java project,
however the [changelog](CHANGELOG.md) will make it clear what version of
OpenTelemetry Java a version is based against.

## Compatibility

For stable components, backwards-incompatible changes will be avoided. If they
are necessary, the `MAJOR` version of the artifact will be incremented.

This can occur in situations including:

- OpenTelemetry Java Instrumentation breaking change - please see [upstream
  repository](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/VERSIONING.md)
  for their versioning policy
- Change in a Splunk-managed configuration property or code

In the latter case, we will first **deprecate** (at least one release earlier)
before introducing the change, so that customers can prepare accordingly.

All the changes are to be communicated in the [changelog](CHANGELOG.md).

### Old versions support

Splunk is committed to support the customers using this distribution. All major
versions will get critical (for example security) patches for **one year**
after the release date. Feature development will stop on a major release once a
new major release is introduced.

## Release cadence

Currently, the `OpenTelemetry Java Instrumentation` is released on a
**monthly** cadence. We strive to release the Splunk distribution with 2
working days after the upstream.
