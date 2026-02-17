pluginManagement {
  plugins {
    id("com.bmuschko.docker-remote-api") version "10.0.0"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("com.github.jk1.dependency-license-report") version "3.1.1"
    id("com.gradleup.shadow") version "9.3.1"
  }
}

plugins {
  id("com.gradle.develocity") version "4.3.2"
}

develocity {
  buildScan {
    termsOfUseUrl = "https://gradle.com/terms-of-service"
    termsOfUseAgree = if (System.getenv("CI") != null) "yes" else "no"

    if (!gradle.startParameter.taskNames.contains(":metadata-generator:generateMetadata")) {
      buildScanPublished {
        File("build-scan.txt").printWriter().use { writer ->
          writer.println(buildScanUri)
        }
      }
    }
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
    "instrumentation:jdbc",
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
