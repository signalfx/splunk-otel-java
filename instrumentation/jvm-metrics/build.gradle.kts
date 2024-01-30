plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  compileOnly(project(":custom"))
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
}

tasks.withType<Test>().configureEach {
  jvmArgs("-Dotel.instrumentation.jvm-metrics.splunk.enabled=true")
}
