pluginManagement {
  plugins {
    id("com.bmuschko.docker-remote-api") version "9.4.0"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("com.github.jk1.dependency-license-report") version "2.9"
    id("com.gradleup.shadow") version "8.3.7"
  }
}

plugins {
  id("com.gradle.develocity") version "4.0.2"
}

develocity {
  buildScan {
    termsOfUseUrl = "https://gradle.com/terms-of-service"
    termsOfUseAgree = if (System.getenv("CI") != null) "yes" else "no"
  }
}

rootProject.name = "splunk-otel-java"
include(":dependencyManagement")
include(
    "agent",
    "agent-csa-bundle",
    "bootstrap",
    "custom",
    "instrumentation",
    "instrumentation:compile-stub",
    "instrumentation:glassfish",
    "instrumentation:jetty",
    "instrumentation:jvm-metrics",
    "instrumentation:khttp",
    "instrumentation:liberty",
    "instrumentation:nocode",
    "instrumentation:nocode-testing",
    "instrumentation:servlet-3-testing",
    "instrumentation:tomcat",
    "instrumentation:tomee",
    "instrumentation:weblogic",
    "instrumentation:websphere",
    "instrumentation:wildfly",
    "metadata-generator",
    "matrix",
    "profiler",
    "smoke-tests",
    "testing:agent-for-testing",
    "testing:agent-test-extension",
    "testing:jmh-benchmarks",
    "testing:common")
