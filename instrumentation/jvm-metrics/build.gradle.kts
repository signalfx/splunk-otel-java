plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  compileOnly(project(":custom"))
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")
    jvmArgs("-Dsplunk.metrics.experimental.enabled=true")
  }
}
