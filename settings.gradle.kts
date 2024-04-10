pluginManagement {
  plugins {
    id("com.bmuschko.docker-remote-api") version "9.4.0"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("com.github.jk1.dependency-license-report") version "2.6"
  }
}

// We are using the following instead of having id("com.github.johnrengelman.shadow") version "8.1.1"
// in pluginManagement to make shadow plugin use a newer version of asm so that it wouldn't fail on
// classes compiled for jdk 21 (jackson-core 2.16.1 has such classes). This can be removed when
// shadow plugin is updated.
buildscript {
  dependencies {
    classpath("com.github.johnrengelman:shadow:8.1.1")
    classpath("org.ow2.asm:asm:9.7")
    classpath("org.ow2.asm:asm-commons:9.7")
  }
}

plugins {
  id("com.gradle.develocity") version "3.17.1"
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
    "bootstrap",
    "custom",
    "instrumentation",
    "instrumentation:common",
    "instrumentation:compile-stub",
    "instrumentation:glassfish",
    "instrumentation:jetty",
    "instrumentation:jvm-metrics",
    "instrumentation:khttp",
    "instrumentation:liberty",
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
