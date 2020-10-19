# Splunk distribution of OpenTelemetry Java Instrumentation Buildpack

A [CloudFoundry buildpack](https://docs.run.pivotal.io/buildpacks/) to install
and run the Splunk distribution of the OpenTelemetry Java Instrumentation agent in CloudFoundry apps.

> :construction: This project is currently in **BETA**.

## Installation

**This buildpack is created automatically when the Splunk distribution of OpenTelemetry Java Instrumentation tile is installed via Ops Manager on Pivotal Platform.**
That is the preferred installation route.

To build and install the buildpack without using the tile you need to have
[cfcli](https://docs.cloudfoundry.org/cf-cli/install-go-cli.html) installed.

If you would like to install the buildpack, clone this repo, change to this directory, then run:

```sh
$ cf create-buildpack splunk_otel_java_buildpack . 99 --enable
```

Now you can use the buildpack when running your apps:

```sh
# app configuration
$ cf set-env my-app OTEL_ZIPKIN_SERVICE_NAME <application name>
# ...

# java_buildpack is the main buildpack for JVM apps, it needs to be the final one
$ cf push my-app -b splunk_otel_java_buildpack -b https://github.com/cloudfoundry/java-buildpack
```

## Configuration

You can configure the Java instrumentation agent using environment variables listed in the [main README.md](../../../README.md).
All configuration options listed there are supported by this buildpack.

If you want to use a specific version of the Java agent in your application, you can set the `SPLUNK_OTEL_JAVA_VERSION`
environment variable before application deployment, either using `cf set-env` or the `manifest.yml` file:

```sh
$ cf set-env SPLUNK_OTEL_JAVA_VERSION "0.1.0"
```

By default the latest available agent version is used.

Note: the latest version won't use the buildpack cache, so if you want your deployments to be a bit quicker you may want to specify a concrete version.
