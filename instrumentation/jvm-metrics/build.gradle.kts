plugins {
  id("splunk.instrumentation-conventions")
}

dependencies {
  compileOnly(project(":custom"))
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
}

tasks {
  val testMicrometerJvmMetrics by registering(Test::class) {
    filter {
      includeTestsMatching("*MicrometerJvmMetricsTest")
    }
    include("**/*MicrometerJvmMetricsTest.*")
    jvmArgs("-Dsplunk.metrics.implementation=micrometer")
  }

  val testOtelJvmMetrics by registering(Test::class) {
    filter {
      includeTestsMatching("*OtelJvmMetricsTest")
    }
    include("**/*OtelJvmMetricsTest.*")
    jvmArgs("-Dsplunk.metrics.implementation=opentelemetry")
  }

  test {
    filter {
      excludeTestsMatching("*MicrometerJvmMetricsTest")
      excludeTestsMatching("*OtelJvmMetricsTest")
      isFailOnNoMatchingTests = false
    }
  }

  test {
    dependsOn(testMicrometerJvmMetrics)
    dependsOn(testOtelJvmMetrics)
  }

  withType<Test>().configureEach {
    jvmArgs("-Dsplunk.metrics.enabled=true")
    jvmArgs("-Dsplunk.metrics.experimental.enabled=true")
  }
}
