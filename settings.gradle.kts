pluginManagement {
  plugins {
    id("com.bmuschko.docker-remote-api") version "7.1.0"
    id("com.diffplug.spotless") version "5.14.0"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("nebula.release") version "15.3.1"
    id("org.gradle.test-retry") version "1.3.1"
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
    "instrumentation:common",
    "instrumentation:commons-dbcp2",
    "instrumentation:compile-stub",
    "instrumentation:glassfish",
    "instrumentation:hikaricp",
    "instrumentation:jetty",
    "instrumentation:jvm-metrics",
    "instrumentation:khttp",
    "instrumentation:liberty",
    "instrumentation:netty-3.8",
    "instrumentation:netty-4.0",
    "instrumentation:servlet",
    "instrumentation:servlet-3-testing",
    "instrumentation:tomcat",
    "instrumentation:tomcat-jdbc",
    "instrumentation:tomee",
    "instrumentation:weblogic",
    "instrumentation:wildfly",
    "matrix",
    "profiler",
    "smoke-tests",
    "testing:agent-for-testing",
    "testing:agent-test-extension",
    "testing:profiler-tests",
    "testing:common")