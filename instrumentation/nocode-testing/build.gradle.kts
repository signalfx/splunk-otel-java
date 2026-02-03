plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  testImplementation(project(":bootstrap"))
  testImplementation(project(":custom"))
  testImplementation(project(":instrumentation:nocode"))
  testImplementation("org.snakeyaml:snakeyaml-engine")
}

tasks {
  compileTestJava {
    dependsOn(":instrumentation:nocode:byteBuddyJava")
  }
}
