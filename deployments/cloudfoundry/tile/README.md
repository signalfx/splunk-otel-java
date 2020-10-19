# Splunk distribution of OpenTelemetry Java Instrumentation for Pivotal Platform

A [PCF tile](https://docs.pivotal.io/tiledev/2-2/index.html) to install
the buildpack for the Splunk distribution of OpenTelemetry Java Instrumentation.

> :construction: This project is currently in **BETA**.

## Installation

To build the tile you need to have the [tile generator](https://github.com/cf-platform-eng/tile-generator/releases)
and [bosh](https://bosh.io/docs/cli-v2-install/) installed.

If you would like to build the tile, clone this repo, change to this directory,
then run the following command with the new version:

```sh
$ ./build.sh NEW_VERSION
```

E.g.

```sh
$ ./build.sh 0.1.0
```

The resulting `.pivotal` file will be located in the `product/`.
You can import this file in the Pivotal Ops Manager on the Installation Dashboard page.
This allows you to install the Splunk distribution of OpenTelemetry Java Instrumentation buildpack on your PCF instance.
