plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  testInstrumentation(project(":instrumentation:micrometer-1.3"))
  testInstrumentation(project(":instrumentation:micrometer-1.5"))

  // use the agent (latest) micrometer version in the default test task
  testImplementation("io.micrometer:micrometer-core")
}

testing {
  suites {
    (3..10).forEach { ver ->
      register<JvmTestSuite>("version1_${ver}Test") {
        sources {
          java {
            setSrcDirs(listOf("src/test/java"))
          }
        }
        dependencies {
          implementation("io.micrometer:micrometer-core:1.$ver.+")

          implementation(project(":testing:common"))
          implementation("org.assertj:assertj-core")
        }

        targets {
          all {
            testTask.configure {
              jvmArgs("-Dsplunk.metrics.enabled=true")

              // set some global metrics tags for testing javaagent
              jvmArgs("-Dsplunk.testing.metrics.global-tags=food=cheesecake")
            }
          }
        }
      }
    }
  }
}

tasks {
  check {
    dependsOn(testing.suites)
  }

  withType<Test> {
    jvmArgs("-Dsplunk.metrics.enabled=true")

    // set some global metrics tags for testing javaagent
    jvmArgs("-Dsplunk.testing.metrics.global-tags=food=cheesecake")
  }
}
