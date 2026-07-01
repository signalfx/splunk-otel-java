plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
}

tasks.withType<Test>().configureEach {
  jvmArgs("-Dotel.instrumentation.jvm-metrics-splunk.enabled=true")
}
