plugins {
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

// it's not really possible to use the muzzle-check plugin here - we're instrumenting a (temporarily) shaded micrometer

dependencies {
  compileOnly(project(":instrumentation:micrometer-shaded-for-instrumenting", configuration = "shadow"))

  testImplementation("io.micrometer:micrometer-core")
}

tasks {
  test {
    jvmArgs("-Dsplunk.metrics.enabled=true")

    // set some global metrics tags for testing javaagent
    jvmArgs("-Dsplunk.testing.metrics.global-tags=food=cheesecake")
  }
}
