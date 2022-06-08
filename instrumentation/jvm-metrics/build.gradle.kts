plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  compileOnly(project(":custom"))
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly("org.slf4j:slf4j-api")
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")
    jvmArgs("-Dsplunk.metrics.experimental.enabled=true")
    jvmArgs("-Dsplunk.metrics.otel.enabled=true")
  }
}
