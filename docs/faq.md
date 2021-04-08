# Frequently Asked Questions

## Why don't you publish the javaagent jar to a Maven repository?

It would make it very easy to accidentally put the agent on the application
runtime classpath, which may cause all sorts of problems and confusion - and
the agent won't work anyway, because it has to be passed in the `-javaagent`
JVM parameter.

## How often do you release?

We strive to release the Splunk distribution at most 2 days after the
[upstream project](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases)
releases. We release a new version every 2-4 weeks.
