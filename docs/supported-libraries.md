> The official Splunk documentation for this page is [Requirements for the Java agent](https://docs.splunk.com/Observability/gdi/get-data-in/application/java/java-otel-requirements.html). For instructions on how to contribute to the docs, see [CONTRIBUTE.md](../CONTRIBUTE.md).

# Supported libraries, frameworks, application servers, and JVMs

The Java Agent instruments numerous libraries, frameworks, application servers. You can find a list of
them [here](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/supported-libraries.md).

Aside from the instrumentations listed in the link above, the Splunk Distribution of OpenTelemetry Java provides a few
additional custom features:

* We instrument [several HTTP server frameworks](server-trace-info.md#frameworks-and-libraries)
  and return server trace information in the HTTP response;
* We collect information about [application servers](webengine-attributes.md) that are being used and store it in
  server span attributes;
* We gather [basic application and JVM metrics](metrics.md) and export it to the Smart Agent, the OpenTelemetry
  Collector or Splunk ingest.
