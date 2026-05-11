plugins {
  id("splunk.java-conventions")
}

dependencies {
  compileOnly(project(":custom"))
  compileOnly(project(":profiler"))
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-declarative-config")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")

  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service")

  implementation("io.opentelemetry.contrib:opentelemetry-opamp-client")

  testImplementation(project(":custom"))
  testImplementation(testFixtures(project(":custom")))
  testImplementation(project(":profiler"))
  testImplementation(project(":testing:common"))
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv")
  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv-incubating")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-testing-common")
}

tasks {
  compileJava {
    options.release.set(8)
  }
}
