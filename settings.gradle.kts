pluginManagement {
  plugins {
    id("com.bmuschko.docker-remote-api") version "7.1.0"
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("nebula.release") version "15.3.1"
    id("org.gradle.test-retry") version "1.3.1"
    id("org.unbroken-dome.test-sets") version "4.0.0"
  }
}

plugins {
  id("com.gradle.enterprise") version "3.4.1"
}

gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
  }
}

rootProject.name = "splunk-otel-java"
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
    "instrumentation:khttp",
    "instrumentation:liberty",
    "instrumentation:micrometer",
    "instrumentation:micrometer-shaded-for-instrumenting",
    "instrumentation:micrometer-testing",
    "instrumentation:netty-3.8",
    "instrumentation:netty-4.0",
    "instrumentation:netty-4.1",
    "instrumentation:servlet",
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
    "testing:profiler-tests",
    "testing:common")