dependencies {
  testImplementation(project(":bootstrap"))
  testImplementation(project(":instrumentation:nocode"))
  testImplementation(testFixtures(project(":custom")))
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  testImplementation("org.snakeyaml:snakeyaml-engine")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  testImplementation("io.github.netmikey.logunit:logunit-jul")
}

tasks {
  compileTestJava {
    dependsOn(":instrumentation:nocode:byteBuddyJava")
  }
}
