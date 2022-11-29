plugins {
  id("splunk.instrumentation-conventions")
  id("org.unbroken-dome.test-sets")
}

testSets {
  (3..10).forEach { ver ->
    create("version1${ver}Test") {
      dirName = "test"
    }
  }
}

dependencies {
  testInstrumentation(project(":instrumentation:micrometer-1.3"))
  testInstrumentation(project(":instrumentation:micrometer-1.5"))

  add("version13TestImplementation", "io.micrometer:micrometer-core:1.3.16")
  add("version14TestImplementation", "io.micrometer:micrometer-core:1.4.2")
  add("version15TestImplementation", "io.micrometer:micrometer-core:1.5.17")
  add("version16TestImplementation", "io.micrometer:micrometer-core:1.6.12")
  add("version17TestImplementation", "io.micrometer:micrometer-core:1.7.5")
  add("version18TestImplementation", "io.micrometer:micrometer-core:1.8.11")
  add("version19TestImplementation", "io.micrometer:micrometer-core:1.9.5")
  add("version110TestImplementation", "io.micrometer:micrometer-core:1.10.2")
  // use the agent (latest) micrometer version in the default test task
  testImplementation("io.micrometer:micrometer-core")
}

tasks {
  test {
    (3..10).forEach { ver ->
      dependsOn(named("version1${ver}Test"))
    }
  }

  withType<Test> {
    jvmArgs("-Dsplunk.metrics.enabled=true")

    // set some global metrics tags for testing javaagent
    jvmArgs("-Dsplunk.testing.metrics.global-tags=food=cheesecake")
  }
}
