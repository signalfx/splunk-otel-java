pluginManagement {
  plugins {
    id("com.bmuschko.docker-remote-api") version "9.3.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
    id("nebula.release") version "17.2.1"
  }
}

plugins {
  id("com.gradle.enterprise") version "3.12.6"
}

gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = if (System.getenv("CI") != null) "yes" else "no"
  }
}

rootProject.name = "splunk-otel-java"
include(":dependencyManagement")
include(
    "agent",
    "bootstrap",
    "custom",
    "instrumentation",
    "instrumentation:c3p0",
    "instrumentation:common",
    "instrumentation:commons-dbcp2",
    "instrumentation:compile-stub",
    "instrumentation:glassfish",
    "instrumentation:hikaricp",
    "instrumentation:jetty",
    "instrumentation:jvm-metrics",
    "instrumentation:khttp-0.1",
    "instrumentation:liberty",
    "instrumentation:micrometer-1.3",
    "instrumentation:micrometer-1.3-shaded-for-instrumenting",
    "instrumentation:micrometer-1.5",
    "instrumentation:micrometer-1.5-shaded-for-instrumenting",
    "instrumentation:micrometer-common",
    "instrumentation:micrometer-testing",
    "instrumentation:oracle-ucp",
    "instrumentation:servlet-3-testing",
    "instrumentation:tomcat",
    "instrumentation:tomcat-jdbc",
    "instrumentation:tomee",
    "instrumentation:vibur-dbcp",
    "instrumentation:weblogic",
    "instrumentation:websphere",
    "instrumentation:wildfly",
    "matrix",
    "profiler",
    "smoke-tests",
    "testing:agent-for-testing",
    "testing:agent-test-extension",
    "testing:jmh-benchmarks",
    "testing:common")
