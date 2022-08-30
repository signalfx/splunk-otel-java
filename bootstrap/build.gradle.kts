dependencies {
  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")

  // required to access OpenTelemetryAgent
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap")

  // add micrometer to the bootstrap classloader so that it's available in instrumentations
  implementation("io.micrometer:micrometer-core")

  compileOnly("io.opentelemetry:opentelemetry-api")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")

  testImplementation("io.opentelemetry:opentelemetry-api")
}

tasks {
  compileJava {
    options.release.set(8)
  }
}
