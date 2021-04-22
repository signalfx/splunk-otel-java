# Splunk Distribution of OpenTelemetry Java Instrumentation Versioning

## Version numbers

The project strictly follows [Semantic Versioning 2.0.0](https://semver.org/). This means
that all artifacts have a version of the format `MAJOR.MINOR.PATCH`.
No backwards-incompatible changes will be made unless incrementing the `MAJOR` version number. 
Most releases will be made by incrementing the `MINOR` version. 
Patch releases will be made by incrementing the `PATCH` version.

## Compatibility  

Backwards-incompatible changes will be avoided - if they are necessary, the `MAJOR` version of the artifact will be
incremented.

This can occur in following situations:
- OpenTelemetry Java Instrumentation breaking change - please see [upstream repository](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/VERSIONING.md) for their versioning policy
- change in a Splunk-managed configuration property or code

In the latter case, we will first **deprecate** (at least one release earlier) before introducing the change, so that customers can prepare accordingly.

All the changes are to be communicated in the [changelog](CHANGELOG.md).

### Old versions support

Splunk is committed to support the customers using this distribution. All major versions will get critical (eg security) patches for **one year** after the release date. 

## Release cadence

Currently, the `OpenTelemetry Java Instrumentation` is released on a **monthly** cadence. We strive to release the Splunk distribution at most 2 working days after the upstream.

