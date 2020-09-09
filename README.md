# SignalFx distribution of OpenTelemetry Java instrumentation

## Getting Started

Download the [latest version](https://github.com/signalfx/signalfx-otel-java/releases/latest/download/signalfx-otel-javaagent-all.jar).

This package includes the instrumentation agent, instrumentations for all supported libraries and all available data exporters.
This provides completely automatic out of the box experience.

The instrumentation agent is enabled using the `-javaagent` flag to the JVM.

```
java -javaagent:path/to/signalfx-otel-javaagent-all.jar \
     -jar myapp.jar
```
