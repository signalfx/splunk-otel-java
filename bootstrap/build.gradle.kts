dependencies {
  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")

  // required to access OpenTelemetryAgent
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap")

  compileOnly("io.opentelemetry:opentelemetry-api")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")

  testImplementation("io.opentelemetry:opentelemetry-api")
}

tasks {
  compileJava {
    options.release.set(8)
  }
}
