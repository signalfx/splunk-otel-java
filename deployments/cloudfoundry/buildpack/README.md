# Splunk distribution of OpenTelemetry Java Instrumentation Buildpack

A [CloudFoundry buildpack](https://docs.run.pivotal.io/buildpacks/) to install
and run the Splunk distribution of OpenTelemetry Java Instrumentation agent in CloudFoundry apps.

## Installation

If you like to install the buildpack, clone this repo and change to this directory, then run:

```sh
$ cf create-buildpack splunk_otel_java_buildpack . 99 --enable
```

Now you can use it when running your apps:

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

In case you want to use a specific version of the Java agent in your application you can set the `SPLUNK_OTEL_JAVA_VERSION` environment variable:

```sh
$ sf set-env SPLUNK_OTEL_JAVA_VERSION "0.1.0"
```

By default the latest agent version available is used.

Note: latest version is not using the buildpack cache, so if you want your deployments to be a bit quicker you may want to specify a concrete version.
