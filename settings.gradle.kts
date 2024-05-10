pluginManagement {
  plugins {
    id("com.bmuschko.docker-remote-api") version "9.4.0"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("com.github.jk1.dependency-license-report") version "2.7"
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
  id("com.gradle.develocity") version "3.17.3"
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
