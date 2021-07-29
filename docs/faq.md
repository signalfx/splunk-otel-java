> The official Splunk documentation for this page is [About the Splunk Distribution of OpenTelemetry Java](https://docs.splunk.com/Observability/gdi/get-data-in/application/java/splunk-java-otel-distribution.html#nav-About-Splunk-OTel-Java).

# Frequently Asked Questions

- **Can upstream opentelemetry-java or opentelemetry-java-instrumentation be
  used instead?** Definitely, however Splunk only provides best-effort support.
- **What’s different between Splunk Distribution of OpenTelemetry Java and
  OpenTelemetry Instrumentation for Java?** Supported by Splunk, better defaults
  for Splunk products, access to other open-source projects including
  Micrometer. Note, we take an upstream-first approach, Splunk Distribution of
  OpenTelemetry Java allow us to move fast.
- **Why don't you publish the javaagent jar to a Maven repository?** It would
  make it very easy to accidentally put the agent on the application runtime
  classpath, which may cause all sorts of problems and confusion - and the
  agent won't work anyway, because it has to be passed in the `-javaagent` JVM
  parameter.
- **How often do you release?** We strive to release the Splunk distribution
  within 2 working days after the [upstream
  project](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases)
  releases. OpenTelemetry Java generally releases a new version every 4 weeks.
