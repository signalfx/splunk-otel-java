plugins {
  id("splunk.java-conventions")
}

dependencies {
  implementation("io.opentelemetry:opentelemetry-api")
  implementation("io.opentelemetry:opentelemetry-sdk-logs")
}

tasks {
  compileJava {
    options.release.set(8)
  }
}
