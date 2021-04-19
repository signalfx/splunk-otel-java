# Supported libraries, frameworks, application servers, and JVMs

The Java Agent instruments numerous libraries, frameworks, application servers.
You can find a list of them [here](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/supported-libraries.md).

Aside from the instrumentations listed in the link above, the Splunk Distribution
of OpenTelemetry Java Instrumentation provides a few additional custom features:

* We instrument [several HTTP server frameworks](server-trace-info.md#supported-frameworks-and-libraries)
  and return server trace information in the HTTP response;
* We collect information about [application servers](middleware-attributes.md) that are being used
  and store it in server span attributes;
* We gather [basic application and JVM metrics](metrics.md) and export it to the Smart Agent,
  the OpenTelemetry Collector or Splunk ingest.
