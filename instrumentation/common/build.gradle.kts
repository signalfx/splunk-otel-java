dependencies {
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry:opentelemetry-api")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  compileOnly("io.micrometer:micrometer-core")

  testImplementation("io.opentelemetry.javaagent:opentelemetry-testing-common")
}
