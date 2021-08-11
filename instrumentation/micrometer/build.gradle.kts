plugins {
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

// it's not really possible to use the muzzle-check plugin here - we're instrumenting a (temporarily) shaded micrometer

dependencies {
  compileOnly(project(":instrumentation:micrometer-shaded-for-instrumenting", configuration = "shadow"))
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")
  }
}
