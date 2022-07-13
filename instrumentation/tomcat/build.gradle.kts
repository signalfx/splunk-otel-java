plugins {
  id("splunk.instrumentation-conventions")
  id("splunk.muzzle-conventions")
}

muzzle {
  pass {
    group.set("org.apache.tomcat")
    module.set("tomcat-catalina")
    versions.set("[8,)")
    // no assertInverse because metrics and attributes instrumentations support different version ranges
  }
}

dependencies {
  compileOnly("org.apache.tomcat:tomcat-catalina:9.0.40")
  implementation(project(":instrumentation:common"))
  compileOnly("org.slf4j:slf4j-api")

  testImplementation("org.apache.tomcat.embed:tomcat-embed-core:9.0.40")
}

tasks {
  val testMicrometerMetrics by registering(Test::class) {
    filter {
      includeTestsMatching("*TomcatMicrometerMetricsTest")
    }
    include("**/*TomcatMicrometerMetricsTest.*")
    jvmArgs("-Dsplunk.metrics.implementation=micrometer")
  }

  val testOtelMetrics by registering(Test::class) {
    filter {
      includeTestsMatching("*TomcatOtelMetricsTest")
    }
    include("**/*TomcatOtelMetricsTest.*")
    jvmArgs("-Dsplunk.metrics.implementation=opentelemetry")
  }

  test {
    filter {
      excludeTestsMatching("*TomcatMicrometerMetricsTest")
      excludeTestsMatching("*TomcatOtelMetricsTest")
      isFailOnNoMatchingTests = false
    }
  }

  test {
    dependsOn(testMicrometerMetrics)
    dependsOn(testOtelMetrics)
  }

  withType<Test>().configureEach {
    jvmArgs("-Dsplunk.metrics.enabled=true")
  }
}
