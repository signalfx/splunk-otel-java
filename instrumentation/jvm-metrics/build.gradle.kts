plugins {
  id("splunk.instrumentation-conventions")
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")
  }
}
