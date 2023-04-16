plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  testInstrumentation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-common")
  testInstrumentation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-2.2")
  testInstrumentation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-servlet-3.0")

  testImplementation("io.opentelemetry.javaagent:opentelemetry-testing-common")

  testImplementation("javax.servlet:javax.servlet-api:3.0.1")
  testImplementation("org.eclipse.jetty:jetty-server:8.0.0.v20110901")
  testImplementation("org.eclipse.jetty:jetty-servlet:8.0.0.v20110901")
}
