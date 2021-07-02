plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  compileOnly("io.netty:netty:3.8.0.Final")
  compileOnly("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-3.8")

  implementation(project(":instrumentation:common"))

  testInstrumentation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-3.8")
  testInstrumentation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.0")
  testInstrumentation("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.1")
  testInstrumentation(project(":instrumentation:netty-4.0"))

  testImplementation("io.netty:netty:3.8.0.Final")
}
