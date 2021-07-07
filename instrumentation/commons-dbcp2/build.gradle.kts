plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  compileOnly("org.apache.commons:commons-dbcp2:2.0")

  testImplementation("org.apache.commons:commons-dbcp2:2.0")
  testImplementation("org.awaitility:awaitility")
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")
  }
}
