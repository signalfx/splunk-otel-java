plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  compileOnly(project(":custom"))
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
}

tasks.withType<Test>().configureEach {
  jvmArgs("-Dsplunk.metrics.enabled=true")
  jvmArgs("-Dsplunk.metrics.experimental.enabled=true")
}
