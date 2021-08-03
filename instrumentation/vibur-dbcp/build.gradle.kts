plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  compileOnly("org.vibur:vibur-dbcp:20.0")

  testImplementation("org.vibur:vibur-dbcp:20.0")
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")
  }
}
