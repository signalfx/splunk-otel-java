plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  testImplementation(project(":bootstrap"))
  testImplementation(project(":custom"))
  testImplementation(project(":instrumentation:nocode"))
  testCompileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  testRuntimeOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling") {
    exclude("io.opentelemetry.javaagent", "opentelemetry-javaagent-bootstrap")
  }
  testImplementation("org.snakeyaml:snakeyaml-engine")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
}

tasks {
  compileTestJava {
    dependsOn(":instrumentation:nocode:byteBuddyJava")
  }
}
