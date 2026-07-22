plugins {
  id("splunk.java-conventions")
}

dependencies {
  implementation(project(":profiler"))
  implementation("com.google.protobuf:protobuf-java")
  implementation("io.opentelemetry:opentelemetry-api")
  implementation("io.opentelemetry:opentelemetry-sdk-logs")
}

tasks {
  compileJava {
    options.release.set(8)
  }
}
