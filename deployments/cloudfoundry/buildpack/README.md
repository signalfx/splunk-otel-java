# Splunk Distribution of OpenTelemetry Java Buildpack

A [CloudFoundry buildpack](https://docs.cloudfoundry.org/buildpacks/) to install and run the Splunk Distribution of the
OpenTelemetry Java agent in CloudFoundry apps.

> :construction: This project is currently in **BETA**. It is **officially supported** by Splunk. However, breaking changes **MAY** be introduced.

## Installation

To build and install the buildpack without using the tile you need to have
[cfcli](https://docs.cloudfoundry.org/cf-cli/install-go-cli.html) installed.

If you would like to install the buildpack, clone this repo, change to this directory, then run:

```sh
$ ./build.sh

# installs the buildpack on CloudFoundry
$ cf create-buildpack splunk_otel_java_buildpack splunk_otel_java_buildpack-linux.zip 99
```

Now you can use the buildpack when running your apps:

```sh
# app configuration
$ cf set-env my-app OTEL_RESOURCE_ATTRIBUTES "service.name=<application name>"
# ...

# java_buildpack is the main buildpack for JVM apps, it needs to be the final one
$ cf push my-app -b splunk_otel_java_buildpack -b https://github.com/cloudfoundry/java-buildpack
```

## Configuration

Please read the [Getting Started](../../../README.md#get-started) to learn how to configure the Splunk Distribution
of OpenTelemetry Java.
[All javaagent configuration options](../../../docs/advanced-config.md) there are supported by this buildpack.

If you want to use a specific version of the Java agent in your application, you can set the `SPLUNK_OTEL_JAVA_VERSION`
environment variable before application deployment, either using `cf set-env` or the `manifest.yml` file:

```sh
$ cf set-env SPLUNK_OTEL_JAVA_VERSION 2.25.0
```

By default, the [latest](https://github.com/signalfx/splunk-otel-java/releases/latest) available agent version is used.

Note: the latest version won't use the buildpack cache, so if you want your deployments to be a bit quicker you may want
to specify a concrete version.
