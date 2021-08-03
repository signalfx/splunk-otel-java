plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  compileOnly("org.apache.commons:commons-dbcp2:2.0")

  testImplementation("org.apache.commons:commons-dbcp2:2.0")
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")
  }
}
