> The official Splunk documentation for this page is [About the Splunk Distribution of OpenTelemetry Java](https://docs.splunk.com/Observability/gdi/get-data-in/application/java/splunk-java-otel-distribution.html#nav-About-Splunk-OTel-Java).

# Frequently Asked Questions

- **Can upstream opentelemetry-java or opentelemetry-java-instrumentation be used instead?** Definitely, however Splunk
  only provides best-effort support.
- **What’s different between Splunk Distribution of OpenTelemetry Java and OpenTelemetry Instrumentation for Java?**
  Supported by Splunk, better defaults for Splunk products, access to other open-source projects including Micrometer.
  Note, we take an upstream-first approach, Splunk Distribution of OpenTelemetry Java allow us to move fast.
- **Should I add the agent as a Maven/Gradle dependency to my application?** No, you definitely shouldn't! The agent
  will not function at all if it's just added to the classpath; it should never be added to the runtime classpath
  directly. The only way to instrument the application automatically with the agent is to use the `-javaagent` command
  line parameter.
- **How often do you release?** We strive to release the Splunk distribution within 2 working days after
  the [upstream project](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases)
  releases. OpenTelemetry Java generally releases a new version every 4 weeks.
- **What happens when the exporter endpoint is temporarily unreachable?** Sending telemetry resumes when the endpoint
  becomes available again. Telemetry collected in the meantime is dropped. To avoid dropping data, use the
  [Splunk OpenTelemetry Connector](https://github.com/signalfx/splunk-otel-collector), which supports retrying failed
  transmissions.