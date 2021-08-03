plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  compileOnly("com.mchange:c3p0:0.9.5")

  testImplementation("com.mchange:c3p0:0.9.5")
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")
  }
}
