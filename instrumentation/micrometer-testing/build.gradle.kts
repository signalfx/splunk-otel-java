plugins {
  id("splunk.instrumentation-conventions")
  id("org.unbroken-dome.test-sets")
}

testSets {
  // TODO: change to 13..16 when 1.3 instrumentation is implemented
  (15..16).forEach { ver ->
    create("version${ver}Test") {
      dirName = "test"
    }
  }
}

dependencies {
  testInstrumentation(project(":instrumentation:micrometer"))

  // TODO: uncomment when 1.3 instrumentation is implemented
//  add("version13TestImplementation", "io.micrometer:micrometer-core:1.3.16")
//  add("version14TestImplementation", "io.micrometer:micrometer-core:1.4.2")
  add("version15TestImplementation", "io.micrometer:micrometer-core:1.5.17")
  add("version16TestImplementation", "io.micrometer:micrometer-core:1.6.12")
  // use the agent (latest) micrometer version in the default test task
  testImplementation("io.micrometer:micrometer-core")
}

tasks {
  test {
    // TODO: change to 13..16 when 1.3 instrumentation is implemented
    (15..16).forEach { ver ->
      dependsOn(named("version${ver}Test"))
    }
  }

  withType<Test> {
    jvmArgs("-Dsplunk.metrics.enabled=true")

    // set some global metrics tags for testing javaagent
    jvmArgs("-Dsplunk.testing.metrics.global-tags=food=cheesecake")
  }
}
