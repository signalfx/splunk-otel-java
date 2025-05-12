//plugins {
//  id("splunk.instrumentation-conventions")
//}

dependencies {
  testImplementation(project(":bootstrap"))
  testImplementation(project(":instrumentation:nocode"))
  testImplementation("org.snakeyaml:snakeyaml-engine:2.8")
}

tasks {
  compileTestJava {
    dependsOn(":instrumentation:nocode:byteBuddyJava")
  }
}
