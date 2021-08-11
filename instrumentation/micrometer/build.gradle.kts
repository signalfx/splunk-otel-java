plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  compileOnly(project(":instrumentation:micrometer-shaded-for-instrumenting", configuration = "shadow"))
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")
  }
}
