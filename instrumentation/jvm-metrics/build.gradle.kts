plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  compileOnly(project(":custom"))
  compileOnly(project(":profiler"))
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")

  testImplementation(testFixtures(project(":custom")))
  testImplementation(project(":profiler"))
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
}

tasks.withType<Test>().configureEach {
  jvmArgs("-Dotel.instrumentation.jvm-metrics-splunk.enabled=true")
}
